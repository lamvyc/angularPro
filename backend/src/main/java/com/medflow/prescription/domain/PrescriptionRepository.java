package com.medflow.prescription.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.prescription.domain.PrescriptionModels.Drug;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.Item;
import com.medflow.prescription.domain.PrescriptionModels.Reservation;

public interface PrescriptionRepository {

    List<Drug> listDrugs();

    Optional<Drug> findDrug(long id);

    long insertPrescription(String no, long encounterId, long patientId, long deptId, long doctorId,
            String type, String diagnosisSummary, String medicalAdvice, long operatorId);

    int updateHeader(long id, String type, String diagnosisSummary, String medicalAdvice, int version,
            long operatorId);

    void replaceItems(long prescriptionId, List<Item> items);

    void deleteItems(long prescriptionId);

    Optional<Header> findHeader(long id);

    List<Header> listHeaders(Long encounterId, String status, Long doctorId);

    List<Item> listItems(long prescriptionId);

    int submit(long id, String fromStatus, int version, Instant at, long operatorId);

    int startReview(long id, int version, long reviewerId, Instant at);

    int approve(long id, int version, long reviewerId, Instant at, Instant expiresAt);

    int reject(long id, int version, long reviewerId, String reason, Instant at);

    int cancel(long id, int version, String reason, long operatorId, Instant at);

    int expire(long id, Instant at);

    List<Header> findExpired(LocalDate date, Instant now);

    void insertRevision(long prescriptionId, int revisionNo, String contentJson, String hash,
            long submittedBy, Instant at);

    void insertReview(long prescriptionId, int revisionNo, long reviewerId, String decision, String reason,
            Instant at);

    void insertEvent(long prescriptionId, String action, String fromStatus, String toStatus, long actorId,
            String reason, String payloadJson);

    // stock
    List<Batch> batchesForDrug(long drugId);

    int reserveBatch(long batchId, int quantity);

    int releaseBatch(long batchId, int quantity);

    int availableStock(long drugId);

    void insertReservation(long prescriptionId, long itemId, long drugId, long batchId, int quantity,
            String idemKey);

    List<Reservation> heldReservations(long prescriptionId);

    int releaseReservation(long reservationId);

    void insertMovement(String movementNo, long drugId, long batchId, String type, int quantity, int beforeQty,
            int afterQty, String bizType, long bizId, String idemKey, Long operatorId, String remark);

    long lastInsertId();

    record Batch(long id, long drugId, String batchNo, LocalDate expiryDate, int stockQty, int reservedQty) {
        public int available() {
            return stockQty - reservedQty;
        }
    }
}
