package com.medflow.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank(message = "科室编码不能为空")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{1,31}", message = "科室编码格式不正确")
        String code,

        @NotBlank(message = "科室名称不能为空")
        @Size(max = 64)
        String name,

        Long parentId,

        @Pattern(regexp = "CLINICAL|PHARMACY|ADMIN", message = "科室类型不正确")
        String type,

        int sortNo) {
}
