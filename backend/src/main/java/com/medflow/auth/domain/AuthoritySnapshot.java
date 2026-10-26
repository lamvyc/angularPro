package com.medflow.auth.domain;

import java.util.List;

import com.medflow.common.security.DataScope;
import com.medflow.iam.domain.PermissionInfo;
import com.medflow.iam.domain.RoleInfo;

public record AuthoritySnapshot(
        long userId,
        String username,
        String realName,
        Long deptId,
        String deptName,
        String title,
        String status,
        DataScope dataScope,
        List<RoleInfo> roles,
        List<String> permissions,
        List<PermissionInfo> menus) {

    public AuthoritySnapshot {
        roles = roles == null ? List.of() : List.copyOf(roles);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        menus = menus == null ? List.of() : List.copyOf(menus);
    }
}
