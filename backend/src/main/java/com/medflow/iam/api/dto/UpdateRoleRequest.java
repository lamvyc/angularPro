package com.medflow.iam.api.dto;

import com.medflow.common.security.DataScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64)
        String name,

        @NotNull(message = "数据范围不能为空")
        DataScope dataScope,

        @Size(max = 255)
        String remark) {
}
