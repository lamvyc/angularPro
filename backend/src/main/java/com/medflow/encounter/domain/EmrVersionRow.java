package com.medflow.encounter.domain;

import java.time.Instant;

public record EmrVersionRow(int versionNo, String status, String contentJson, String contentHash,
        Long signedBy, String signedByName, Instant signedAt, String changeReason, Instant createdAt) {
}
