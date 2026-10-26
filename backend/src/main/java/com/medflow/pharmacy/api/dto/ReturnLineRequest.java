package com.medflow.pharmacy.api.dto;

import jakarta.validation.constraints.Min;

public record ReturnLineRequest(@Min(1) long reservationId, @Min(1) int quantity) {
}
