package com.medflow.encounter.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmrTemplateRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank String scopeType,
        Long deptId,
        @NotNull Map<String, Object> schema,
        @Size(max = 255) String changelog) {
}
