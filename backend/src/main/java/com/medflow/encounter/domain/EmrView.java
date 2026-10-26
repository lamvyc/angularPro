package com.medflow.encounter.domain;

import java.time.Instant;
import java.util.Map;

public record EmrView(long id, String recordNo, String status, Map<String, Object> content, int version,
        Instant updatedAt) {
}
