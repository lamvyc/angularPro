package com.medflow.pharmacy.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ReturnRequest(
        @Size(max = 255) String reason,
        @NotEmpty List<@Valid ReturnLineRequest> lines) {
}
