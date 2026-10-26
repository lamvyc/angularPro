package com.medflow.pharmacy.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.appointment.application.BusinessNumberService;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.iam.application.AuditService;
import com.medflow.pharmacy.domain.PharmacyModels;
import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.ReturnLine;
import com.medflow.pharmacy.domain.PharmacyRepository;
import com.medflow.prescription.domain.PrescriptionModels;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.View;
import com.medflow.prescription.domain.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class PharmacyTxService {

    private final PharmacyRepository pharmacy;
    private final PrescriptionRepository prescriptions;
    private final BusinessNumberService numbers;
    private final AuditService auditService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional
    public DrugAdmin createDrug(String code, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal price, String prescriptionType) {
        if (code == null || code.isBlank() || name == null || name.isBlank() || spec == null
                || unit == null || price == null || price.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "药品编码、名称、规格、单位、价格不能为空");
        }
        long id;
        try {
            id = pharmacy.insertDrug(code.trim().toUpperCase(), name.trim(), trimToNull(genericName), spec.trim(),
                    unit.trim(), trimToNull(dosageForm), trimToNull(manufacturer),
                    "HERBAL".equals(category) ? "HERBAL" : "WESTERN", price,
                    "OTC".equals(prescriptionType) ? "OTC" : "RX");
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "药品编码已存在");
        }
        auditService.success("DRUG_CREATED", "DRUG", String.valueOf(id), null,
                mapOf("code", code, "name", name));
        return pharmacy.findDrug(id).orElseThrow();
    }

    @Transactional
    public DrugAdmin updateDrug(long id, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal price, String prescriptionType) {
        DrugAdmin before = pharmacy.findDrug(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "药品不存在"));
        if (name == null || name.isBlank() || spec == null || unit == null || price == null || price.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "药品名称、规格、单位、价格不能为空");
        }
        pharmacy.updateDrug(id, name.trim(), trimToNull(genericName), spec.trim(), unit.trim(),
                trimToNull(dosageForm), trimToNull(manufacturer),
                "HERBAL".equals(category) ? "HERBAL" : "WESTERN", price,
                "OTC".equals(prescriptionType) ? "OTC" : "RX");
        auditService.success("DRUG_UPDATED", "DRUG", String.valueOf(id),
                mapOf("name", before.name(), "price", before.unitPrice()),
                mapOf("name", name, "price", price));
        return pharmacy.findDrug(id).orElseThrow();
    }

    @Transactional
    public DrugAdmin setDrugStatus(long id, String status) {
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "药品状态不合法");
        }
        DrugAdmin before = pharmacy.findDrug(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "药品不存在"));
        pharmacy.setDrugStatus(id, status);
        auditService.success("DRUG_STATUS_CHANGED", "DRUG", String.valueOf(id), mapOf("status", before.status()),
                mapOf("status", status));
        return pharmacy.findDrug(id).orElseThrow();
    }

    @Transactional
    public BatchView createBatch(long drugId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
            int quantity) {
        if (batchNo == null || batchNo.isBlank() || expiryDate == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "批号、有效期、数量不能为空且数量必须大于 0");
        }
        if (expiryDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "过期批次不能入库");
        }
        if (productionDate != null && productionDate.isAfter(expiryDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "生产日期不能晚于有效期");
        }
        DrugAdmin drug = pharmacy.findDrug(drugId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "药品不存在"));
        if (!"ENABLED".equals(drug.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "停用药品不能入库");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        long batchId;
        try {
            batchId = pharmacy.insertBatch(drugId, batchNo.trim(), productionDate, expiryDate, quantity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "同一药品批号已存在");
        }
        pharmacy.insertMovement(numbers.next("MV"), drugId, batchId, "IN", quantity, 0, quantity, "BATCH", batchId,
                "IN:" + batchId, user.id(), "批次入库");
        auditService.success("STOCK_INBOUND", "BATCH", String.valueOf(batchId), null,
                mapOf("drugId", drugId, "quantity", quantity, "expiryDate", expiryDate));
        return pharmacy.findBatch(batchId).orElseThrow();
    }

    @Transactional
    public View dispense(long prescriptionId) {
        Header header = prescriptions.findHeader(prescriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "处方不存在"));
        if ("DISPENSED".equals(header.status())) {
            return view(header, prescriptions.listItems(prescriptionId));
        }
        if (!"APPROVED".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已通过处方可以发药");
        }
        List<ReservationView> reservations = pharmacy.reservations(prescriptionId, "HELD");
        if (reservations.isEmpty()) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "处方没有有效预占");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (pharmacy.dispensePrescription(prescriptionId, user.id(), now) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        for (ReservationView reservation : reservations) {
            if (reservation.expiryDate().isBefore(LocalDate.now(clock))) {
                throw new BusinessException(ErrorCode.EXPIRED, "批次 " + reservation.batchNo() + " 已过期");
            }
            int before = pharmacy.findBatch(reservation.batchId()).map(BatchView::available).orElse(0);
            if (pharmacy.consumeBatch(reservation.batchId(), reservation.quantity()) == 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "库存扣减失败，请重试");
            }
            pharmacy.markReservationConsumed(reservation.id());
            pharmacy.insertMovement(numbers.next("MV"), reservation.drugId(), reservation.batchId(), "DISPENSE",
                    reservation.quantity(), before, before - reservation.quantity(), "PRESCRIPTION",
                    prescriptionId, "DISP:" + prescriptionId + ":" + reservation.id(), user.id(), "发药扣减");
        }
        prescriptions.insertEvent(prescriptionId, "DISPENSE", "APPROVED", "DISPENSED", user.id(), null, "{}");
        auditService.success("PRESCRIPTION_DISPENSED", "PRESCRIPTION", String.valueOf(prescriptionId),
                mapOf("status", "APPROVED"), mapOf("status", "DISPENSED", "reservations", reservations.size()));
        return view(prescriptions.findHeader(prescriptionId).orElseThrow(),
                prescriptions.listItems(prescriptionId));
    }

    @Transactional
    public View returnStock(long prescriptionId, String idempotencyKey, String reason, List<ReturnLine> lines) {
        Header header = prescriptions.findHeader(prescriptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "处方不存在"));
        if (!"DISPENSED".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已发药处方可以退药");
        }
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少选择一条退药明细");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        for (ReturnLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "退药数量必须大于 0");
            }
            ReservationView reservation = pharmacy.findReservation(line.reservationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "预占明细不存在"));
            if (reservation.prescriptionId() != prescriptionId || !"CONSUMED".equals(reservation.status())) {
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "预占明细不属于该处方或未发药");
            }
            if (reservation.returnedQty() + line.quantity() > reservation.quantity()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "退药数量超过已发数量：" + reservation.drugName());
            }
            int before = pharmacy.findBatch(reservation.batchId()).map(BatchView::available).orElse(0);
            if (pharmacy.addReturned(reservation.id(), line.quantity()) == 0) {
                throw new BusinessException(ErrorCode.VERSION_CONFLICT, "退药数量冲突，请刷新后重试");
            }
            pharmacy.returnBatch(reservation.batchId(), line.quantity());
            pharmacy.insertMovement(numbers.next("MV"), reservation.drugId(), reservation.batchId(), "RETURN",
                    line.quantity(), before, before + line.quantity(), "PRESCRIPTION", prescriptionId,
                    "RET:" + prescriptionId + ":" + idempotencyKey + ":" + reservation.id(), user.id(),
                    "退药" + (reservation.expiryDate().isBefore(LocalDate.now(clock)) ? "（过期批次转入隔离）" : ""));
        }
        prescriptions.insertEvent(prescriptionId, "RETURN", "DISPENSED", "DISPENSED", user.id(), reason, "{}");
        auditService.success("PRESCRIPTION_RETURNED", "PRESCRIPTION", String.valueOf(prescriptionId), null,
                mapOf("lines", lines.size(), "reason", reason));
        return view(prescriptions.findHeader(prescriptionId).orElseThrow(),
                prescriptions.listItems(prescriptionId));
    }

    private View view(Header header, List<PrescriptionModels.Item> items) {
        return new View(header, items);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
