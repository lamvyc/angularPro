package com.medflow.encounter.domain;

import java.time.Instant;

public record EmrRecordRow(long id, String recordNo, String status, String contentJson, int version,
        Instant updatedAt, long templateId, long templateVersionId, String templateName, String schemaJson,
        Instant signedAt, Long signedBy, String signatureHash, Instant amendedAt, String amendReason) {
}
