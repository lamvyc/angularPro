package com.medflow.iam.domain;

import java.time.Instant;

/**
 * 登录与安全上下文所需的用户读模型。
 */
public record UserAccount(
        long id,
        String username,
        String passwordHash,
        String realName,
        String employeeNo,
        Long deptId,
        String deptName,
        String title,
        String phone,
        String status,
        int failedAttempts,
        Instant lockedUntil,
        Instant lastLoginAt,
        int version) {
}
