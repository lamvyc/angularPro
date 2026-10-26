package com.medflow.pharmacy.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PharmacyModels {

    private PharmacyModels() {
    }

    public record DrugAdmin(long id, String code, String name, String genericName, String spec, String unit,
            String dosageForm, String manufacturer, String category, BigDecimal unitPrice,
            String prescriptionType, String status) {
    }

    public record BatchView(long id, long drugId, String drugName, String batchNo, LocalDate productionDate,
            LocalDate expiryDate, int stockQty, int reservedQty, int available, String status) {
    }

    public record MovementView(long id, String movementNo, String drugName, String batchNo, String type,
            int quantity, int beforeQty, int afterQty, String bizType, long bizId, String remark, Instant createdAt) {
    }

    public record Summary(long drugId, String drugName, int stockQty, int reservedQty, int available, int batches) {
    }

    public record ReservationView(long id, long prescriptionId, long itemId, long drugId, String drugName,
            long batchId, String batchNo, LocalDate expiryDate, int quantity, int returnedQty, String status) {
    }

    public record ReturnLine(long reservationId, int quantity) {
    }
}
