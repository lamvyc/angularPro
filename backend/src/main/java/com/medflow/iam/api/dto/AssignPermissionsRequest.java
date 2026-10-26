package com.medflow.iam.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignPermissionsRequest(
        @NotNull(message = "权限列表不能为空")
        List<Long> permissionIds) {
}
