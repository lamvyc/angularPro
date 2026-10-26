package com.medflow.prescription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PrescriptionModels {

    private PrescriptionModels() {
    }

    public enum Status {
        DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED, EXPIRED, DISPENSED, COMPLETED
    }

    public record Drug(long id, String code, String name, String spec, String unit, BigDecimal unitPrice,
            String prescriptionType) {
    }

    public record ItemCommand(long drugId, String dosage, String frequency, String route, int days,
            int quantity, String note) {
    }

    public record CreateCommand(String type, String diagnosisSummary, String medicalAdvice,
            List<ItemCommand> items) {
    }

    public record Item(long id, long drugId, String drugName, String spec, String dosage, String frequency,
            String route, int days, int quantity, String unit, BigDecimal unitPrice, BigDecimal amount,
            String note) {
    }

    public record Header(long id, String prescriptionNo, long encounterId, long patientId, String patientName,
            long deptId, String deptName, long doctorId, String doctorName, String type, String status,
            int revisionNo, String diagnosisSummary, String medicalAdvice, Instant submittedAt, Long reviewedBy,
            String reviewerName, Instant reviewedAt, String rejectReason, Instant expiresAt, Instant createdAt,
            int version) {
    }

    public record View(Header header, List<Item> items) {
        public View {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record Reservation(long id, long prescriptionId, long itemId, long drugId, long batchId, int quantity,
            String status) {
    }
}
