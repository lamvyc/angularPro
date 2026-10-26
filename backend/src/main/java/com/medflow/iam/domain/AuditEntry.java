package com.medflow.iam.domain;

public record AuditEntry(
        String traceId,
        Long actorId,
        String actorName,
        String action,
        String resourceType,
        String resourceId,
        String result,
        String errorCode,
        String beforeJson,
        String afterJson,
        String ip,
        String userAgent) {
}
