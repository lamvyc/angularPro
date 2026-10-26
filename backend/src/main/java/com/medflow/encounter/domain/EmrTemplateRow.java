package com.medflow.encounter.domain;

import java.time.Instant;

public record EmrTemplateRow(long id, String code, String name, String scopeType, Long deptId,
        Long currentVersionId, int versionNo, String schemaJson, String changelog, String status,
        Instant publishedAt) {
}
