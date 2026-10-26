package com.medflow.pharmacy.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchRequest(
        @Min(1) long drugId,
        @NotBlank @Size(max = 64) String batchNo,
        LocalDate productionDate,
        @NotNull LocalDate expiryDate,
        @Min(1) int quantity) {
}
