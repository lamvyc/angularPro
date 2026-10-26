package com.medflow.iam.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50)
        String realName,

        @NotNull(message = "科室不能为空")
        Long deptId,

        @Size(max = 32)
        String title,

        @Size(max = 20)
        String phone,

        @NotEmpty(message = "至少分配一个角色")
        List<Long> roleIds) {
}
