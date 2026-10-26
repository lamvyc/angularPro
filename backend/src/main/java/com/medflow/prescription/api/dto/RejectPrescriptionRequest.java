package com.medflow.prescription.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectPrescriptionRequest(@NotNull @Min(0) Integer version,
        @NotBlank @Size(max = 255) String reason) {
}
