package com.medflow.encounter.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrRecordRow;
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
    public long insertEmr(long encounterId, long patientId, long doctorId, String recordNo, String contentJson) {
        mapper.insertEmr(encounterId, patientId, doctorId, recordNo, contentJson);
        return mapper.lastInsertId();
    }

    @Override
    public int updateEmr(long id, String contentJson, int version) {
        return mapper.updateEmr(id, contentJson, version, null);
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
