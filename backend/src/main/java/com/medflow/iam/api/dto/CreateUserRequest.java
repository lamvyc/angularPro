package com.medflow.iam.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "用户名格式不正确")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 50)
        String realName,

        @Size(max = 32)
        String employeeNo,

        @NotNull(message = "科室不能为空")
        Long deptId,

        @Size(max = 32)
        String title,

        @Size(max = 20)
        String phone,

        @NotEmpty(message = "至少分配一个角色")
        List<Long> roleIds) {
}
