package com.medflow.pharmacy.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
import com.medflow.pharmacy.domain.PharmacyModels.MovementView;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.Summary;

public interface PharmacyRepository {

    List<DrugAdmin> listDrugs();

    Optional<DrugAdmin> findDrug(long id);

    long insertDrug(String code, String name, String genericName, String spec, String unit, String dosageForm,
            String manufacturer, String category, java.math.BigDecimal price, String prescriptionType);

    int updateDrug(long id, String name, String genericName, String spec, String unit, String dosageForm,
            String manufacturer, String category, java.math.BigDecimal price, String prescriptionType);

    int setDrugStatus(long id, String status);

    List<BatchView> listBatches(Long drugId);

    Optional<BatchView> findBatch(long id);

    long insertBatch(long drugId, String batchNo, LocalDate productionDate, LocalDate expiryDate, int quantity);

    int consumeBatch(long batchId, int quantity);

    int returnBatch(long batchId, int quantity);

    List<Summary> summary();

    List<MovementView> movements(int limit);

    void insertMovement(String movementNo, long drugId, long batchId, String type, int quantity, int beforeQty,
            int afterQty, String bizType, long bizId, String idemKey, Long operatorId, String remark);

    List<ReservationView> reservations(long prescriptionId, String status);

    Optional<ReservationView> findReservation(long id);

    int markReservationConsumed(long id);

    int addReturned(long id, int quantity);

    int dispensePrescription(long id, long operatorId, Instant at);

    long lastInsertId();
}
