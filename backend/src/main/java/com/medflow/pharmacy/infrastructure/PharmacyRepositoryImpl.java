package com.medflow.pharmacy.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
import com.medflow.pharmacy.domain.PharmacyModels.MovementView;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.Summary;
import com.medflow.pharmacy.domain.PharmacyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PharmacyRepositoryImpl implements PharmacyRepository {

    private final PharmacyMapper mapper;

    @Override
    public List<DrugAdmin> listDrugs() {
        return mapper.listDrugs();
    }

    @Override
    public Optional<DrugAdmin> findDrug(long id) {
        return mapper.findDrug(id);
    }

    @Override
    public long insertDrug(String code, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal price, String prescriptionType) {
        mapper.insertDrug(code, name, genericName, spec, unit, dosageForm, manufacturer, category, price,
                prescriptionType);
        return mapper.lastInsertId();
    }

    @Override
    public int updateDrug(long id, String name, String genericName, String spec, String unit, String dosageForm,
            String manufacturer, String category, BigDecimal price, String prescriptionType) {
        return mapper.updateDrug(id, name, genericName, spec, unit, dosageForm, manufacturer, category, price,
                prescriptionType);
    }

    @Override
    public int setDrugStatus(long id, String status) {
        return mapper.setDrugStatus(id, status);
    }

    @Override
    public List<BatchView> listBatches(Long drugId) {
        return mapper.listBatches(drugId);
    }

    @Override
    public Optional<BatchView> findBatch(long id) {
        return mapper.findBatch(id);
    }

    @Override
    public long insertBatch(long drugId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
            int quantity) {
        mapper.insertBatch(drugId, batchNo, productionDate, expiryDate, quantity);
        return mapper.lastInsertId();
    }

    @Override
    public int consumeBatch(long batchId, int quantity) {
        return mapper.consumeBatch(batchId, quantity);
    }

    @Override
    public int returnBatch(long batchId, int quantity) {
        return mapper.returnBatch(batchId, quantity);
    }

    @Override
    public List<Summary> summary() {
        return mapper.summary();
    }

    @Override
    public List<MovementView> movements(int limit) {
        return mapper.movements(Math.min(200, Math.max(1, limit)));
    }

    @Override
    public void insertMovement(String movementNo, long drugId, long batchId, String type, int quantity,
            int beforeQty, int afterQty, String bizType, long bizId, String idemKey, Long operatorId,
            String remark) {
        mapper.insertMovement(movementNo, drugId, batchId, type, quantity, beforeQty, afterQty, bizType, bizId,
                idemKey, operatorId, remark);
    }

    @Override
    public List<ReservationView> reservations(long prescriptionId, String status) {
        return mapper.reservations(prescriptionId, status);
    }

    @Override
    public Optional<ReservationView> findReservation(long id) {
        return mapper.findReservation(id);
    }

    @Override
    public int markReservationConsumed(long id) {
        return mapper.markReservationConsumed(id);
    }

    @Override
    public int addReturned(long id, int quantity) {
        return mapper.addReturned(id, quantity);
    }

    @Override
    public int dispensePrescription(long id, long operatorId, Instant at) {
        return mapper.dispensePrescription(id, operatorId, at);
    }

    @Override
    public long lastInsertId() {
        return mapper.lastInsertId();
    }
}
