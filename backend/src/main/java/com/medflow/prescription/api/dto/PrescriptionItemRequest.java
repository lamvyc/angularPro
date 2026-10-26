package com.medflow.prescription.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(
        @Min(1) long drugId,
        @NotBlank @Size(max = 64) String dosage,
        @NotBlank @Size(max = 64) String frequency,
        @NotBlank @Size(max = 32) String route,
        @Min(1) int days,
        @Min(1) int quantity,
        @Size(max = 255) String note) {
}
