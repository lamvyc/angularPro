package com.medflow.encounter.domain;

import java.time.Instant;
import java.util.Map;

public record EmrDraftView(Map<String, Object> content, int version, Instant updatedAt) {
}
