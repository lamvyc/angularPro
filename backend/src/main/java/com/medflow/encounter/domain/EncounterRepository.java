package com.medflow.encounter.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EncounterRepository {

    List<EncounterRow> queue(LocalDate date, Long doctorId, Long deptId);

    Optional<EncounterRow> find(long id);

    int start(long id, Instant at);

    int finish(long id, Instant at);

    int cancel(long id, String reason);

    int updateAppointmentStatus(long appointmentId, String fromStatus, String toStatus);

    void insertEvent(long encounterId, String action, String fromStatus, String toStatus, long actorId,
            String reason, String payloadJson);

    Optional<EmrRecordRow> findEmr(long encounterId);

    long insertEmr(long encounterId, long patientId, long doctorId, String recordNo, String contentJson,
            long templateId, long templateVersionId);

    int updateEmr(long id, String contentJson, int version);

    int signEmr(long id, int version, String contentJson, String hash, long signerId, Instant at);

    int amendEmr(long id, int version, String contentJson, String hash, long signerId, Instant at,
            String reason);

    void supersedeVersions(long recordId);

    long insertVersion(long recordId, int versionNo, String contentJson, String hash, String status,
            Long authorId, Long signedBy, Instant signedAt, String changeReason);

    List<EmrVersionRow> listVersions(long recordId);

    Optional<EmrVersionRow> findVersion(long recordId, int versionNo);

    Optional<EmrTemplateRow> findTemplateForDept(long deptId);

    Optional<EmrTemplateRow> findTemplate(long id);

    List<EmrTemplateRow> listCurrentTemplates();

    List<EmrTemplateRow> listTemplateVersions(long templateId);

    long insertTemplate(String code, String name, String scopeType, Long deptId, long operatorId);

    long insertTemplateVersion(long templateId, int versionNo, String schemaJson, String changelog,
            long operatorId, Instant at);

    void setTemplateCurrentVersion(long templateId, long versionId, long operatorId);

    void archiveTemplate(long templateId, long operatorId);

    void deleteDiagnoses(long encounterId);

    void insertDiagnosis(long encounterId, String code, String name, String type, String note, int sortNo,
            Long operatorId);

    List<DiagnosisView> listDiagnoses(long encounterId);

    long lastInsertId();
}
