package com.medflow.iam.domain;

public record NewUser(
        String username,
        String passwordHash,
        String realName,
        String employeeNo,
        Long deptId,
        String title,
        String phone) {
}
