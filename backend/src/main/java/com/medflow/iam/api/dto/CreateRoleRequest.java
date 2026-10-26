package com.medflow.iam.api.dto;

import com.medflow.common.security.DataScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{2,31}", message = "角色编码格式不正确")
        String code,

        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64)
        String name,

        @NotNull(message = "数据范围不能为空")
        DataScope dataScope,

        @Size(max = 255)
        String remark) {
}
