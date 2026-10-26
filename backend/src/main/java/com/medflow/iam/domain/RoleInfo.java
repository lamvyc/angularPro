package com.medflow.iam.domain;

import java.util.List;

import com.medflow.common.security.DataScope;

public record RoleInfo(long id, String code, String name, DataScope dataScope, boolean builtIn, String status,
        String remark) {

    public boolean enabled() {
        return "ENABLED".equals(status);
    }

    public static List<String> codes(List<RoleInfo> roles) {
        return roles.stream().map(RoleInfo::code).toList();
    }
}
