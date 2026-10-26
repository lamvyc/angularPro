package com.medflow.encounter.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record EmrSaveRequest(@NotNull Map<String, Object> content, @NotNull Integer version) {
}
