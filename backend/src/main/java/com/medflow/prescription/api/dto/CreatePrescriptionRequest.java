package com.medflow.prescription.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreatePrescriptionRequest(
        String type,
        @Size(max = 500) String diagnosisSummary,
        @Size(max = 500) String medicalAdvice,
        @NotEmpty List<@Valid PrescriptionItemRequest> items) {
}
