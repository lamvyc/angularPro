package com.medflow.encounter.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DiagnosesRequest(
        @NotEmpty(message = "至少一条诊断")
        List<@Valid DiagnosisInput> diagnoses) {

    public record DiagnosisInput(
            @Size(max = 32) String code,
            @Size(max = 100) String name,
            @Pattern(regexp = "PRIMARY|SECONDARY", message = "诊断类型只能是 PRIMARY 或 SECONDARY")
            String type,
            @Size(max = 255) String note) {
    }
}
