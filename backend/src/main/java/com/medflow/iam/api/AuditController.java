package com.medflow.iam.api;

import java.time.Instant;

import com.medflow.common.web.PageResult;
import com.medflow.iam.application.AuditQueryService;
import com.medflow.iam.domain.AuditLogRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit", description = "审计日志查询（只读）")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService auditQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:audit:read')")
    @Operation(summary = "分页查询审计日志，默认最近 7 天")
    public PageResult<AuditLogRow> search(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return auditQueryService.search(page, size, action, actorId, from, to);
    }
}
