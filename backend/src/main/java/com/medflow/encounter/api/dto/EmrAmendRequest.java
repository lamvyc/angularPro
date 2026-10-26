package com.medflow.encounter.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmrAmendRequest(@NotNull Map<String, Object> content, @NotNull Integer version,
        @NotBlank @Size(max = 255) String reason) {
}
