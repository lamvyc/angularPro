package com.medflow.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
        String newPassword) {
}
