package com.medflow.common.error;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.medflow.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 过滤器链（认证/授权失败）不在 MVC 范围内，需要手动写出 Problem Details。
 */
@Component
public class ProblemResponseWriter {

    private final ProblemDetailsFactory factory;
    private final JsonMapper jsonMapper;

    public ProblemResponseWriter(ProblemDetailsFactory factory, JsonMapper jsonMapper) {
        this.factory = factory;
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String detail) {
        ProblemDetail problem = factory.create(errorCode, request, detail);
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(RequestIdFilter.HEADER, RequestIdFilter.currentRequestId(request));
        try {
            jsonMapper.writeValue(response.getOutputStream(), problem);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
