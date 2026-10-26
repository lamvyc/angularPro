package com.medflow.iam.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.web.PageResult;
import com.medflow.iam.domain.AuditLogRow;
import com.medflow.iam.domain.AuditQuery;
import com.medflow.iam.domain.AuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditRepository auditRepository;
    private final Clock clock;

    public PageResult<AuditLogRow> search(int page, int size, String action, Long actorId,
            Instant from, Instant to) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Instant safeTo = to == null ? Instant.now(clock) : to;
        Instant safeFrom = from == null ? safeTo.minus(Duration.ofDays(7)) : from;
        if (safeFrom.isAfter(safeTo)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "开始时间不能晚于结束时间");
        }
        return auditRepository.search(new AuditQuery(safePage, safeSize, action, actorId, safeFrom, safeTo));
    }
}
