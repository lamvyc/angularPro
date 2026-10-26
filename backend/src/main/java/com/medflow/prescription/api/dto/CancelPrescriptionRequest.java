package com.medflow.prescription.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelPrescriptionRequest(@NotNull @Min(0) Integer version,
        @Size(max = 255) String reason) {
}
