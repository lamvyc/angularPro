package com.medflow.iam.domain;

import com.medflow.common.web.PageResult;

public interface AuditRepository {

    void insert(AuditEntry entry);

    PageResult<AuditLogRow> search(AuditQuery query);
}
