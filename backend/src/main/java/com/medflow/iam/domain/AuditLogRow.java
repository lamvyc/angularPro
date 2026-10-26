package com.medflow.iam.domain;

import java.time.Instant;

public record AuditLogRow(
        long id,
        Instant createdAt,
        Long actorId,
        String actorName,
        String action,
        String resourceType,
        String resourceId,
        String result,
        String errorCode,
        String ip,
        String traceId) {
}
