package com.medflow.iam.domain;

import java.time.Instant;
import java.util.List;

public record UserView(
        long id,
        String username,
        String realName,
        String employeeNo,
        Long deptId,
        String deptName,
        String title,
        String phone,
        String status,
        Instant lastLoginAt,
        List<RoleRef> roles) {

    public UserView {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public record RoleRef(long id, String code, String name) {
    }
}
