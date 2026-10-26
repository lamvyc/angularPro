package com.medflow.encounter.domain;

import java.time.Instant;

public record EmrRecordRow(long id, String recordNo, String status, String contentJson, int version,
        Instant updatedAt) {
}
