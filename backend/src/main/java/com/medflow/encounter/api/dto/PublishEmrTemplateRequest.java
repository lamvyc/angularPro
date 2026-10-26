package com.medflow.encounter.api.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublishEmrTemplateRequest(@NotNull Map<String, Object> schema,
        @Size(max = 255) String changelog) {
}
