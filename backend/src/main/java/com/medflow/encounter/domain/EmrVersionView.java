package com.medflow.encounter.domain;

import java.time.Instant;
import java.util.Map;

public record EmrVersionView(int versionNo, String status, Map<String, Object> content, String contentHash,
        Long signedBy, String signedByName, Instant signedAt, String changeReason, Instant createdAt) {
}
