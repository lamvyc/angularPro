package com.medflow.encounter.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrRecordRow;
import com.medflow.encounter.domain.EmrTemplateRow;
import com.medflow.encounter.domain.EmrVersionRow;
import com.medflow.encounter.domain.EncounterRepository;
import com.medflow.encounter.domain.EncounterRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EncounterRepositoryImpl implements EncounterRepository {

    private final EncounterMapper mapper;

    @Override
    public List<EncounterRow> queue(LocalDate date, Long doctorId, Long deptId) {
        return mapper.queue(date, doctorId, deptId);
    }

    @Override
    public Optional<EncounterRow> find(long id) {
        return mapper.find(id);
    }

    @Override
    public int start(long id, Instant at) {
        return mapper.start(id, at);
    }

    @Override
    public int finish(long id, Instant at) {
        return mapper.finish(id, at);
    }

    @Override
    public int cancel(long id, String reason) {
        return mapper.cancel(id, reason);
    }

    @Override
    public int updateAppointmentStatus(long appointmentId, String fromStatus, String toStatus) {
        return mapper.updateAppointmentStatus(appointmentId, fromStatus, toStatus);
    }

    @Override
    public void insertEvent(long encounterId, String action, String fromStatus, String toStatus, long actorId,
            String reason, String payloadJson) {
        mapper.insertEvent(encounterId, action, fromStatus, toStatus, actorId, reason, payloadJson);
    }

    @Override
    public Optional<EmrRecordRow> findEmr(long encounterId) {
        return mapper.findEmr(encounterId);
    }

    @Override
    public long insertEmr(long encounterId, long patientId, long doctorId, String recordNo,
            String contentJson, long templateId, long templateVersionId) {
        mapper.insertEmr(encounterId, patientId, doctorId, recordNo, contentJson, templateId,
                templateVersionId);
        return mapper.lastInsertId();
    }

    @Override
    public int updateEmr(long id, String contentJson, int version) {
        return mapper.updateEmr(id, contentJson, version, null);
    }

    @Override
    public int signEmr(long id, int version, String contentJson, String hash, long signerId, Instant at) {
        return mapper.signEmr(id, version, contentJson, hash, signerId, at);
    }

    @Override
    public int amendEmr(long id, int version, String contentJson, String hash, long signerId, Instant at,
            String reason) {
        return mapper.amendEmr(id, version, contentJson, hash, signerId, at, reason);
    }

    @Override
    public void supersedeVersions(long recordId) {
        mapper.supersedeVersions(recordId);
    }

    @Override
    public long insertVersion(long recordId, int versionNo, String contentJson, String hash, String status,
            Long authorId, Long signedBy, Instant signedAt, String changeReason) {
        mapper.insertVersion(recordId, versionNo, contentJson, hash, status, authorId, signedBy,
                signedAt, changeReason);
        return mapper.lastInsertId();
    }

    @Override
    public List<EmrVersionRow> listVersions(long recordId) {
        return mapper.listVersions(recordId);
    }

    @Override
    public Optional<EmrVersionRow> findVersion(long recordId, int versionNo) {
        return mapper.findVersion(recordId, versionNo);
    }

    @Override
    public Optional<EmrTemplateRow> findTemplateForDept(long deptId) {
        return mapper.findTemplateForDept(deptId);
    }

    @Override
    public Optional<EmrTemplateRow> findTemplate(long id) {
        return mapper.findTemplate(id);
    }

    @Override
    public List<EmrTemplateRow> listCurrentTemplates() {
        return mapper.listCurrentTemplates();
    }

    @Override
    public List<EmrTemplateRow> listTemplateVersions(long templateId) {
        return mapper.listTemplateVersions(templateId);
    }

    @Override
    public long insertTemplate(String code, String name, String scopeType, Long deptId, long operatorId) {
        mapper.insertTemplate(code, name, scopeType, deptId, operatorId);
        return mapper.lastInsertId();
    }

    @Override
    public long insertTemplateVersion(long templateId, int versionNo, String schemaJson, String changelog,
            long operatorId, Instant at) {
        mapper.insertTemplateVersion(templateId, versionNo, schemaJson, changelog, operatorId, at);
        return mapper.lastInsertId();
    }

    @Override
    public void setTemplateCurrentVersion(long templateId, long versionId, long operatorId) {
        mapper.setTemplateCurrentVersion(templateId, versionId, operatorId);
    }

    @Override
    public void archiveTemplate(long templateId, long operatorId) {
        mapper.archiveTemplate(templateId, operatorId);
    }

    @Override
    public void deleteDiagnoses(long encounterId) {
        mapper.deleteDiagnoses(encounterId);
    }

    @Override
    public void insertDiagnosis(long encounterId, String code, String name, String type, String note, int sortNo,
            Long operatorId) {
        mapper.insertDiagnosis(encounterId, code, name, type, note, sortNo, operatorId);
    }

    @Override
    public List<DiagnosisView> listDiagnoses(long encounterId) {
        return mapper.listDiagnoses(encounterId);
    }

    @Override
    public long lastInsertId() {
        return mapper.lastInsertId();
    }
}
