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

    long insertEmr(long encounterId, long patientId, long doctorId, String recordNo, String contentJson);

    int updateEmr(long id, String contentJson, int version);

    void deleteDiagnoses(long encounterId);

    void insertDiagnosis(long encounterId, String code, String name, String type, String note, int sortNo,
            Long operatorId);

    List<DiagnosisView> listDiagnoses(long encounterId);

    long lastInsertId();
}
