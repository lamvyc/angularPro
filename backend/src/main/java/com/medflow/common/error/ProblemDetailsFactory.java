package com.medflow.common.error;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.medflow.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * 统一构造 RFC 9457 Problem Details，并附加业务扩展字段。
 */
@Component
public class ProblemDetailsFactory {

    public static final String TYPE_PREFIX = "https://medflow.local/problems/";

    public ProblemDetail create(ErrorCode errorCode, HttpServletRequest request, String detail) {
        String message = detail == null || detail.isBlank() ? errorCode.defaultDetail() : detail;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), message);
        problem.setTitle(errorCode.title());
        problem.setType(URI.create(TYPE_PREFIX + errorCode.code()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", errorCode.code());
        String requestId = RequestIdFilter.currentRequestId(request);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        problem.setProperty("fieldErrors", List.<Map<String, String>>of());
        return problem;
    }

    public ProblemDetail withFieldErrors(ProblemDetail problem, List<Map<String, String>> fieldErrors) {
        problem.setProperty("fieldErrors", fieldErrors == null ? List.of() : fieldErrors);
        return problem;
    }
}
