package com.medflow.encounter.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.DataScope;
import com.medflow.common.security.DataScopeResolver;
import com.medflow.common.security.SecurityUtils;
import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrDraftView;
import com.medflow.encounter.domain.EmrRecordRow;
import com.medflow.encounter.domain.EmrTemplateRow;
import com.medflow.encounter.domain.EmrVersionRow;
import com.medflow.encounter.domain.EmrVersionView;
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
    private final EmrSchemaValidator schemaValidator;
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
            EmrTemplateRow template = repository.findTemplateForDept(encounter.deptId())
                    .or(() -> repository.findTemplate(1L))
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "没有可用的病历模板"));
            repository.insertEmr(encounterId, encounter.patientId(), encounter.doctorId(),
                    numbers.next("EMR"), "{}", template.id(), template.currentVersionId());
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
        if (!"DRAFT".equals(record.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "已签署病历不能直接编辑，请发起修订");
        }
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
    public EmrView sign(long encounterId, Map<String, Object> content, int expectedVersion) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        requireInProgress(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历尚未创建"));
        if (!"DRAFT".equals(record.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "病历已签署");
        }
        validateTemplate(record, content);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        String json = jsonMapper.writeValueAsString(content);
        String hash = signatureHash(record, content, user.id(), now);
        if (repository.signEmr(record.id(), expectedVersion, json, hash, user.id(), now) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        int signedVersion = expectedVersion + 1;
        repository.insertVersion(record.id(), signedVersion, json, hash, "SIGNED", user.id(), user.id(), now,
                null);
        draftStore.delete(record.id());
        auditService.success("EMR_SIGNED", "EMR", String.valueOf(record.id()),
                mapOf("version", expectedVersion, "status", "DRAFT"),
                mapOf("version", signedVersion, "status", "SIGNED", "hash", hash));
        return toView(repository.findEmr(encounterId).orElseThrow());
    }

    @Transactional
    public EmrView amend(long encounterId, Map<String, Object> content, int expectedVersion, String reason) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历尚未创建"));
        if (!"SIGNED".equals(record.status()) && !"AMENDED".equals(record.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已签署病历可以修订");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "修订原因不能为空");
        }
        validateTemplate(record, content);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        String json = jsonMapper.writeValueAsString(content);
        String hash = signatureHash(record, content, user.id(), now);
        if (repository.amendEmr(record.id(), expectedVersion, json, hash, user.id(), now, reason.trim()) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        repository.supersedeVersions(record.id());
        int amendedVersion = expectedVersion + 1;
        repository.insertVersion(record.id(), amendedVersion, json, hash, "SIGNED", user.id(), user.id(), now,
                reason.trim());
        draftStore.delete(record.id());
        auditService.success("EMR_REVISED", "EMR", String.valueOf(record.id()),
                mapOf("version", expectedVersion, "status", record.status(), "hash", record.signatureHash()),
                mapOf("version", amendedVersion, "status", "AMENDED", "hash", hash, "reason", reason.trim()));
        return toView(repository.findEmr(encounterId).orElseThrow());
    }

    public List<EmrVersionView> versions(long encounterId) {
        EncounterRow encounter = require(encounterId);
        requireRead(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历不存在"));
        return repository.listVersions(record.id()).stream().map(this::toVersionView).toList();
    }

    public EmrVersionView version(long encounterId, int versionNo) {
        EncounterRow encounter = require(encounterId);
        requireRead(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历不存在"));
        return repository.findVersion(record.id(), versionNo).map(this::toVersionView)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "病历版本不存在"));
    }

    private EmrVersionView toVersionView(EmrVersionRow row) {
        return new EmrVersionView(row.versionNo(), row.status(), parse(row.contentJson()), row.contentHash(),
                row.signedBy(), row.signedByName(), row.signedAt(), row.changeReason(), row.createdAt());
    }

    private void validateTemplate(EmrRecordRow record, Map<String, Object> content) {
        Map<String, Object> schema = parse(record.schemaJson());
        List<Map<String, String>> errors = schemaValidator.validate(schema, content);
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "病历模板校验失败", errors);
        }
    }

    private String signatureHash(EmrRecordRow record, Map<String, Object> content, long signerId, Instant at) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("content", new TreeMap<>(content));
        payload.put("templateVersionId", record.templateVersionId());
        payload.put("signerId", signerId);
        payload.put("signedAt", at.toString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(jsonMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    @Transactional
    public EncounterDetail finish(long encounterId) {
        EncounterRow encounter = require(encounterId);
        requireOwnDoctor(encounter);
        requireInProgress(encounter);
        EmrRecordRow record = repository.findEmr(encounterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STATE_CONFLICT, "结束就诊条件未满足：病历不存在"));
        if (!"SIGNED".equals(record.status()) && !"AMENDED".equals(record.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "结束就诊条件未满足：病历未签署");
        }
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
                record.version(), record.updatedAt(), record.templateId(), record.templateVersionId(),
                record.templateName(), parse(record.schemaJson()), record.signedAt(), record.signedBy(),
                record.signatureHash(), record.amendedAt(), record.amendReason());
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
