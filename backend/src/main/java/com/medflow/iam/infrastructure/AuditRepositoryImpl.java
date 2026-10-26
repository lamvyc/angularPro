package com.medflow.iam.infrastructure;

import com.medflow.common.web.PageResult;
import com.medflow.iam.domain.AuditEntry;
import com.medflow.iam.domain.AuditLogRow;
import com.medflow.iam.domain.AuditQuery;
import com.medflow.iam.domain.AuditRepository;
import com.medflow.iam.infrastructure.persistence.SysAuditLogMapper;
import com.medflow.iam.infrastructure.persistence.SysAuditLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditRepositoryImpl implements AuditRepository {

    private final SysAuditLogMapper mapper;

    @Override
    public void insert(AuditEntry entry) {
        SysAuditLogPO po = new SysAuditLogPO();
        po.setTraceId(entry.traceId());
        po.setActorId(entry.actorId());
        po.setActorName(entry.actorName());
        po.setAction(entry.action());
        po.setResourceType(entry.resourceType());
        po.setResourceId(entry.resourceId());
        po.setResult(entry.result());
        po.setErrorCode(entry.errorCode());
        po.setBeforeJson(entry.beforeJson());
        po.setAfterJson(entry.afterJson());
        po.setIp(entry.ip());
        po.setUserAgent(entry.userAgent());
        mapper.insert(po);
    }

    @Override
    public PageResult<AuditLogRow> search(AuditQuery query) {
        long offset = (long) (query.page() - 1) * query.size();
        return PageResult.of(mapper.search(query, offset, query.size()),
                query.page(), query.size(), mapper.count(query));
    }
}
