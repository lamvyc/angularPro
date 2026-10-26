package com.medflow.pharmacy.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.medflow.appointment.application.IdempotencyService;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
import com.medflow.pharmacy.domain.PharmacyModels.MovementView;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.ReturnLine;
import com.medflow.pharmacy.domain.PharmacyModels.Summary;
import com.medflow.pharmacy.domain.PharmacyRepository;
import com.medflow.prescription.domain.PrescriptionModels.View;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PharmacyAppService {

    private final PharmacyTxService txService;
    private final PharmacyRepository repository;
    private final IdempotencyService idempotencyService;

    public List<DrugAdmin> drugs() {
        return repository.listDrugs();
    }

    public DrugAdmin createDrug(String code, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal price, String prescriptionType) {
        return txService.createDrug(code, name, genericName, spec, unit, dosageForm, manufacturer, category, price,
                prescriptionType);
    }

    public DrugAdmin updateDrug(long id, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal price, String prescriptionType) {
        return txService.updateDrug(id, name, genericName, spec, unit, dosageForm, manufacturer, category, price,
                prescriptionType);
    }

    public DrugAdmin setDrugStatus(long id, String status) {
        return txService.setDrugStatus(id, status);
    }

    public List<BatchView> batches(Long drugId) {
        return repository.listBatches(drugId);
    }

    public BatchView createBatch(long drugId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
            int quantity) {
        return txService.createBatch(drugId, batchNo, productionDate, expiryDate, quantity);
    }

    public List<Summary> summary() {
        return repository.summary();
    }

    public List<MovementView> movements(int limit) {
        return repository.movements(limit);
    }

    public List<ReservationView> reservations(long prescriptionId) {
        return repository.reservations(prescriptionId, null);
    }

    public View dispense(long prescriptionId) {
        return txService.dispense(prescriptionId);
    }

    public IdempotencyService.IdempotentResult<View> returnStock(long prescriptionId, String idempotencyKey,
            String reason, List<ReturnLine> lines) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return idempotencyService.execute(user.id(), idempotencyKey, "POST",
                "/api/v1/prescriptions/" + prescriptionId + "/returns",
                Map.of("lines", lines, "reason", reason == null ? "" : reason), View.class, 200,
                () -> txService.returnStock(prescriptionId, idempotencyKey, reason, lines));
    }
}
