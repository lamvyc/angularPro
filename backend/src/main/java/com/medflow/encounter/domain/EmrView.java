package com.medflow.encounter.domain;

import java.time.Instant;
import java.util.Map;

public record EmrView(long id, String recordNo, String status, Map<String, Object> content, int version,
        Instant updatedAt, long templateId, long templateVersionId, String templateName,
        Map<String, Object> schema, Instant signedAt, Long signedBy, String signatureHash,
        Instant amendedAt, String amendReason) {
}
