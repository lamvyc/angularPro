package com.medflow.auth.domain;

import java.util.List;

import com.medflow.common.security.DataScope;
import com.medflow.iam.domain.MenuNode;

public record UserProfile(
        long id,
        String username,
        String realName,
        Long deptId,
        String deptName,
        String title,
        List<RoleView> roles,
        List<String> permissions,
        DataScope dataScope,
        List<MenuNode> menus) {

    public UserProfile {
        roles = roles == null ? List.of() : List.copyOf(roles);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        menus = menus == null ? List.of() : List.copyOf(menus);
    }

    public record RoleView(String code, String name, DataScope dataScope) {
    }
}
