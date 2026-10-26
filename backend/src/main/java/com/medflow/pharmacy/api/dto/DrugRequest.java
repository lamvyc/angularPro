package com.medflow.pharmacy.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DrugRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String genericName,
        @NotBlank @Size(max = 100) String spec,
        @NotBlank @Size(max = 16) String unit,
        @Size(max = 32) String dosageForm,
        @Size(max = 100) String manufacturer,
        String category,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        String prescriptionType) {
}
