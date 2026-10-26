package com.medflow.iam.domain;

import java.util.List;

import com.medflow.common.security.DataScope;

public record RoleView(
        long id,
        String code,
        String name,
        DataScope dataScope,
        boolean builtIn,
        String status,
        String remark,
        List<Long> permissionIds) {

    public RoleView {
        permissionIds = permissionIds == null ? List.of() : List.copyOf(permissionIds);
    }
}
