package com.medflow.prescription.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PrescriptionVersionRequest(@NotNull @Min(0) Integer version) {
}
