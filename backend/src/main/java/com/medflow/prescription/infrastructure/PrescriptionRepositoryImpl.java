package com.medflow.prescription.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.prescription.domain.PrescriptionModels.Drug;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.Item;
import com.medflow.prescription.domain.PrescriptionModels.Reservation;
import com.medflow.prescription.domain.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PrescriptionRepositoryImpl implements PrescriptionRepository {

    private final PrescriptionMapper mapper;

    @Override
    public List<Drug> listDrugs() {
        return mapper.listDrugs();
    }

    @Override
    public Optional<Drug> findDrug(long id) {
        return mapper.findDrug(id);
    }

    @Override
    public long insertPrescription(String no, long encounterId, long patientId, long deptId, long doctorId,
            String type, String diagnosisSummary, String medicalAdvice, long operatorId) {
        mapper.insertPrescription(no, encounterId, patientId, deptId, doctorId, type, diagnosisSummary,
                medicalAdvice, operatorId);
        return mapper.lastInsertId();
    }

    @Override
    public int updateHeader(long id, String type, String diagnosisSummary, String medicalAdvice, int version,
            long operatorId) {
        return mapper.updateHeader(id, type, diagnosisSummary, medicalAdvice, version, operatorId);
    }

    @Override
    public void replaceItems(long prescriptionId, List<Item> items) {
        mapper.deleteItems(prescriptionId);
        int sort = 0;
        for (Item item : items) {
            mapper.insertItem(prescriptionId, item, sort++);
        }
    }

    @Override
    public void deleteItems(long prescriptionId) {
        mapper.deleteItems(prescriptionId);
    }

    @Override
    public Optional<Header> findHeader(long id) {
        return mapper.findHeader(id);
    }

    @Override
    public List<Header> listHeaders(Long encounterId, String status, Long doctorId) {
        return mapper.listHeaders(encounterId, status, doctorId);
    }

    @Override
    public List<Item> listItems(long prescriptionId) {
        return mapper.listItems(prescriptionId);
    }

    @Override
    public int submit(long id, String fromStatus, int version, Instant at, long operatorId) {
        return mapper.submit(id, fromStatus, version, at, operatorId);
    }

    @Override
    public int startReview(long id, int version, long reviewerId, Instant at) {
        return mapper.startReview(id, version, reviewerId, at);
    }

    @Override
    public int approve(long id, int version, long reviewerId, Instant at, Instant expiresAt) {
        return mapper.approve(id, version, reviewerId, at, expiresAt);
    }

    @Override
    public int reject(long id, int version, long reviewerId, String reason, Instant at) {
        return mapper.reject(id, version, reviewerId, reason, at);
    }

    @Override
    public int cancel(long id, int version, String reason, long operatorId, Instant at) {
        return mapper.cancel(id, version, reason, operatorId, at);
    }

    @Override
    public int expire(long id, Instant at) {
        return mapper.expire(id, at);
    }

    @Override
    public List<Header> findExpired(LocalDate date, Instant now) {
        return mapper.findExpired(now);
    }

    @Override
    public void insertRevision(long prescriptionId, int revisionNo, String contentJson, String hash,
            long submittedBy, Instant at) {
        mapper.insertRevision(prescriptionId, revisionNo, contentJson, hash, submittedBy, at);
    }

    @Override
    public void insertReview(long prescriptionId, int revisionNo, long reviewerId, String decision, String reason,
            Instant at) {
        mapper.insertReview(prescriptionId, revisionNo, reviewerId, decision, reason, at);
    }

    @Override
    public void insertEvent(long prescriptionId, String action, String fromStatus, String toStatus, long actorId,
            String reason, String payloadJson) {
        mapper.insertEvent(prescriptionId, action, fromStatus, toStatus, actorId, reason, payloadJson);
    }

    @Override
    public List<Batch> batchesForDrug(long drugId) {
        return mapper.batchesForDrug(drugId);
    }

    @Override
    public int reserveBatch(long batchId, int quantity) {
        return mapper.reserveBatch(batchId, quantity);
    }

    @Override
    public int releaseBatch(long batchId, int quantity) {
        return mapper.releaseBatch(batchId, quantity);
    }

    @Override
    public int availableStock(long drugId) {
        return mapper.availableStock(drugId);
    }

    @Override
    public void insertReservation(long prescriptionId, long itemId, long drugId, long batchId, int quantity,
            String idemKey) {
        mapper.insertReservation(prescriptionId, itemId, drugId, batchId, quantity, idemKey);
    }

    @Override
    public List<Reservation> heldReservations(long prescriptionId) {
        return mapper.heldReservations(prescriptionId);
    }

    @Override
    public int releaseReservation(long reservationId) {
        return mapper.releaseReservation(reservationId);
    }

    @Override
    public void insertMovement(String movementNo, long drugId, long batchId, String type, int quantity,
            int beforeQty, int afterQty, String bizType, long bizId, String idemKey, Long operatorId,
            String remark) {
        mapper.insertMovement(movementNo, drugId, batchId, type, quantity, beforeQty, afterQty, bizType,
                bizId, idemKey, operatorId, remark);
    }

    @Override
    public long lastInsertId() {
        return mapper.lastInsertId();
    }
}
