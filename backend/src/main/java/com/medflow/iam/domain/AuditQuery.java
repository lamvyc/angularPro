package com.medflow.iam.domain;

import java.time.Instant;

public record AuditQuery(int page, int size, String action, Long actorId, Instant from, Instant to) {
}
