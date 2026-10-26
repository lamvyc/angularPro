package com.medflow.iam.api.dto;

import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能是 ENABLED 或 DISABLED")
        String status) {
}
