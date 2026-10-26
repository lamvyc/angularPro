package com.medflow.iam.domain;

import java.time.Instant;

/**
 * 管理端用户列表行；角色聚合列在仓储层拼接，避免 N+1。
 */
public record UserRow(
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
        String roleIds,
        String roleCodes,
        String roleNames) {
}
