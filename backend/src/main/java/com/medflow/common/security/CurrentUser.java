package com.medflow.common.security;

import java.util.List;

/**
 * 已认证用户在 SecurityContext 中的主体。
 */
public record CurrentUser(
        long id,
        String username,
        String realName,
        Long deptId,
        String deptName,
        String title,
        DataScope dataScope,
        List<String> roles,
        List<String> permissions) {

    public CurrentUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean canAccessDepartment(Long targetDeptId) {
        return switch (dataScope) {
            case ALL -> true;
            case DEPT -> deptId != null && deptId.equals(targetDeptId);
            case SELF -> false;
        };
    }
}
