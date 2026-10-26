package com.medflow.encounter.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.DataScope;
import com.medflow.common.security.DataScopeResolver;
import com.medflow.common.security.SecurityUtils;
import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrDraftView;
import com.medflow.encounter.domain.EmrRecordRow;
import com.medflow.encounter.domain.EmrView;
import com.medflow.encounter.domain.EncounterDetail;
import com.medflow.encounter.domain.EncounterRepository;
import com.medflow.encounter.domain.EncounterRow;
import com.medflow.iam.application.AuditService;
import com.medflow.appointment.application.BusinessNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class EncounterAppService {

    private final EncounterRepository repository;
    private final BusinessNumberService numbers;
    private final DataScopeResolver dataScopeResolver;
    private final EmrDraftStore draftStore;
    private final AuditService auditService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public List<EncounterRow> queue(LocalDate date) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        LocalDate day = date == null ? LocalDate.now(clock) : date;
        Long scopedDept = dataScopeResolver.resolveDepartmentFilter(null).orElse(null);
        Long scopedDoctor = user.roles().contains("DOCTOR") ? Long.valueOf(user.id()) : null;
        return repository.queue(day, scopedDoctor, scopedDept);
    }

    public EncounterDetail detail(long encounterId) {
        EncounterRow encounter = require(encounterId);
        requireRead(encounter);
        EmrRecordRow record = repository.findEmr(encounterId).orElse(null);
        EmrView emr = record == null ? null : toView(record);
        EmrDraftView draft = record == null ? null : draftStore.get(record.id())
                .filter(snapshot -> snapshot.version() >= record.version())
                .map(snapshot -> new EmrDraftView(snapshot.content(), snapshot.version(),
                        snapshot.updatedAt()))
                .orElse(null);
        return new EncounterDetail(encounter, emr, repository.listDiagnoses(encounterId), draft);
    }

    @Transactional
    public EncounterDetail start(long encounterId) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        if (!"WAITING".equals(encounter.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有候诊中的就诊可以开始");
        }
        Instant now = Instant.now(clock);
        if (repository.start(encounterId, now) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        repository.updateAppointmentStatus(encounter.appointmentId(), "CHECKED_IN", "IN_CONSULTATION");
        if (repository.findEmr(encounterId).isEmpty()) {
            repository.insertEmr(encounterId, encounter.patientId(), encounter.doctorId(),
                    numbers.next("EMR"), "{}");
        }
        repository.insertEvent(encounterId, "START", "WAITING", "IN_PROGRESS", encounter.doctorId(), null,
                "{}");
        auditService.success("ENCOUNTER_STARTED", "ENCOUNTER", String.valueOf(encounterId),
                mapOf("status", "WAITING"), mapOf("status", "IN_PROGRESS"));
        return detail(encounterId);
    }

    @Transactional
    public EmrView saveEmr(long encounterId, Map<String, Object> content, int expectedVersion) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        requireInProgress(encounter);
        validateContent(content);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历尚未创建"));
        String json = jsonMapper.writeValueAsString(content);
        if (repository.updateEmr(record.id(), json, expectedVersion) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        EmrRecordRow updated = repository.findEmr(encounterId).orElseThrow();
        draftStore.put(record.id(), content, updated.version(), updated.updatedAt());
        return toView(updated);
    }

    @Transactional
    public List<DiagnosisView> saveDiagnoses(long encounterId, List<DiagnosisCommand> diagnoses) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        requireInProgress(encounter);
        if (diagnoses == null || diagnoses.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少录入一条诊断");
        }
        long primaryCount = diagnoses.stream().filter(item -> "PRIMARY".equals(item.type())).count();
        if (primaryCount != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "必须且只能有一条主诊断");
        }
        repository.deleteDiagnoses(encounterId);
        int sort = 0;
        for (DiagnosisCommand diagnosis : diagnoses) {
            if (diagnosis.code() == null || diagnosis.code().isBlank()
                    || diagnosis.name() == null || diagnosis.name().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "诊断编码与名称不能为空");
            }
            repository.insertDiagnosis(encounterId, diagnosis.code().trim(), diagnosis.name().trim(),
                    diagnosis.type(), diagnosis.note(), sort++, encounter.doctorId());
        }
        repository.insertEvent(encounterId, "DIAGNOSES_SAVED", encounter.status(), encounter.status(),
                encounter.doctorId(), null, jsonMapper.writeValueAsString(mapOf("count", diagnoses.size())));
        auditService.success("ENCOUNTER_DIAGNOSIS_UPDATED", "ENCOUNTER", String.valueOf(encounterId),
                null, mapOf("count", diagnoses.size(), "primary", primaryCount));
        return repository.listDiagnoses(encounterId);
    }

    @Transactional
    public EncounterDetail finish(long encounterId) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        requireInProgress(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT, "结束就诊条件未满足：病历不存在"));
        Map<String, Object> content = parse(record.contentJson());
        Object chiefComplaint = content.get("chiefComplaint");
        if (chiefComplaint == null || chiefComplaint.toString().isBlank()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "结束就诊条件未满足：主诉不能为空");
        }
        List<DiagnosisView> diagnoses = repository.listDiagnoses(encounterId);
        if (diagnoses.stream().noneMatch(item -> "PRIMARY".equals(item.type()))) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "结束就诊条件未满足：缺少主诊断");
        }
        Instant now = Instant.now(clock);
        if (repository.finish(encounterId, now) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        repository.updateAppointmentStatus(encounter.appointmentId(), "IN_CONSULTATION", "COMPLETED");
        repository.insertEvent(encounterId, "FINISH", "IN_PROGRESS", "COMPLETED", encounter.doctorId(), null,
                "{}");
        draftStore.delete(record.id());
        auditService.success("ENCOUNTER_COMPLETED", "ENCOUNTER", String.valueOf(encounterId),
                mapOf("status", "IN_PROGRESS"), mapOf("status", "COMPLETED"));
        return detail(encounterId);
    }

    @Transactional
    public EncounterDetail cancel(long encounterId, String reason) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        if (!"WAITING".equals(encounter.status()) && !"IN_PROGRESS".equals(encounter.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        String appointmentFrom = "IN_PROGRESS".equals(encounter.status()) ? "IN_CONSULTATION" : "CHECKED_IN";
        repository.cancel(encounterId, reason == null ? "医生取消就诊" : reason);
        repository.updateAppointmentStatus(encounter.appointmentId(), appointmentFrom, "CANCELLED");
        repository.insertEvent(encounterId, "CANCEL", encounter.status(), "CANCELLED", encounter.doctorId(),
                reason, "{}");
        auditService.success("ENCOUNTER_CANCELLED", "ENCOUNTER", String.valueOf(encounterId),
                mapOf("status", encounter.status()), mapOf("status", "CANCELLED", "reason", reason));
        return detail(encounterId);
    }

    private void requireRead(EncounterRow encounter) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        boolean ownDoctor = user.id() == encounter.doctorId();
        boolean sameDept = user.dataScope() == DataScope.ALL || user.canAccessDepartment(encounter.deptId());
        if (!ownDoctor && !sameDept) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权访问该就诊");
        }
    }

    private void requireOwnDoctor(EncounterRow encounter) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (user.id() != encounter.doctorId()) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只有接诊医生可以执行该操作");
        }
    }

    private void requireInProgress(EncounterRow encounter) {
        if (!"IN_PROGRESS".equals(encounter.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "就诊不在接诊中");
        }
    }

    private EncounterRow require(long id) {
        return repository.find(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "就诊不存在"));
    }

    private EmrView toView(EmrRecordRow record) {
        return new EmrView(record.id(), record.recordNo(), record.status(), parse(record.contentJson()),
                record.version(), record.updatedAt());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonMapper.readValue(json, Map.class);
    }

    private void validateContent(Map<String, Object> content) {
        if (content == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "病历内容不能为空");
        }
        Object chiefComplaint = content.get("chiefComplaint");
        if (chiefComplaint == null || chiefComplaint.toString().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "主诉不能为空");
        }
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    public record DiagnosisCommand(String code, String name, String type, String note) {
    }
}
