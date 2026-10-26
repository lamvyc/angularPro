package com.medflow.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank(message = "科室名称不能为空")
        @Size(max = 64)
        String name,

        Long parentId,

        @Pattern(regexp = "CLINICAL|PHARMACY|ADMIN", message = "科室类型不正确")
        String type,

        int sortNo) {
}
