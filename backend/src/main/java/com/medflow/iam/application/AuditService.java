package com.medflow.iam.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.common.web.RequestIdFilter;
import com.medflow.iam.domain.AuditEntry;
import com.medflow.iam.domain.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final JsonMapper jsonMapper;

    public void success(String action, String resourceType, String resourceId, Map<String, Object> before,
            Map<String, Object> after) {
        CurrentUser user = SecurityUtils.currentUser().orElse(null);
        record(action, resourceType, resourceId, "SUCCESS", null, before, after,
                user == null ? null : user.id(), user == null ? null : user.realName());
    }

    /**
     * 登录成功时请求尚未建立 SecurityContext，需要显式传入操作人。
     */
    public void successFor(Long actorId, String actorName, String action, String resourceType,
            String resourceId, Map<String, Object> before, Map<String, Object> after) {
        record(action, resourceType, resourceId, "SUCCESS", null, before, after, actorId, actorName);
    }

    /**
     * 失败审计必须独立提交：调用方随后抛出的业务异常会回滚外层事务。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(String action, String resourceType, String resourceId, String errorCode,
            Long actorId, String actorName) {
        record(action, resourceType, resourceId, "FAILURE", errorCode, null, null, actorId, actorName);
    }

    private void record(String action, String resourceType, String resourceId, String result,
            String errorCode, Map<String, Object> before, Map<String, Object> after, Long actorId,
            String actorName) {
        HttpServletRequest request = currentRequest();
        String traceId = request == null ? null : RequestIdFilter.currentRequestId(request);
        String ip = request == null ? null : clientIp(request);
        String userAgent = request == null ? null : truncate(request.getHeader("User-Agent"), 255);
        auditRepository.insert(new AuditEntry(
                traceId,
                actorId,
                actorName,
                action,
                resourceType,
                resourceId,
                result,
                errorCode,
                toJson(before),
                toJson(after),
                ip,
                userAgent));
    }

    private String toJson(Map<String, Object> value) {
        return value == null ? null : jsonMapper.writeValueAsString(value);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes
                ? servletAttributes.getRequest()
                : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public Map<String, Object> change(String field, Object before, Object after) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("field", field);
        map.put("before", before);
        map.put("after", after);
        return map;
    }

    public Map<String, Object> changes(List<Map<String, Object>> changes) {
        return Map.of("changes", changes);
    }
}
