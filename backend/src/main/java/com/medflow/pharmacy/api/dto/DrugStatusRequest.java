package com.medflow.pharmacy.api.dto;

import jakarta.validation.constraints.Pattern;

public record DrugStatusRequest(@Pattern(regexp = "ENABLED|DISABLED") String status) {
}
