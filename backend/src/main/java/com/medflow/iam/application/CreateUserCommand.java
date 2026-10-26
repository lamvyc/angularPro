package com.medflow.iam.application;

import java.util.List;

public record CreateUserCommand(
        String username,
        String password,
        String realName,
        String employeeNo,
        Long deptId,
        String title,
        String phone,
        List<Long> roleIds) {
}
