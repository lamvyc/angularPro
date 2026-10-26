package com.medflow.common.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.iam.application.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 把异常映射为稳定的 Problem Details。前端只依赖 code 与 fieldErrors，不解析文案。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailsFactory problemDetailsFactory;
    private final AuditService auditService;

    public GlobalExceptionHandler(ProblemDetailsFactory problemDetailsFactory, AuditService auditService) {
        this.problemDetailsFactory = problemDetailsFactory;
        this.auditService = auditService;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.debug("business error code={} path={}", ex.errorCode().code(), request.getRequestURI());
        ProblemDetail problem = problemDetailsFactory.create(ex.errorCode(), request, ex.getMessage());
        return ex.fieldErrors().isEmpty() ? problem
                : problemDetailsFactory.withFieldErrors(problem, ex.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", fieldError.getField());
            item.put("message", fieldError.getDefaultMessage());
            fieldErrors.add(item);
        }
        ProblemDetail problem = problemDetailsFactory.create(ErrorCode.VALIDATION_FAILED, request, "请求参数校验失败");
        return problemDetailsFactory.withFieldErrors(problem, fieldErrors);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ProblemDetail handleValidation(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof HttpMessageNotReadableException ? "请求体格式不正确" : "请求参数校验失败";
        return problemDetailsFactory.create(ErrorCode.VALIDATION_FAILED, request, detail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        try {
            auditService.failure("ACCESS_DENIED", "API", request.getRequestURI(),
                    ErrorCode.AUTHZ_DENIED.code(),
                    com.medflow.common.security.SecurityUtils.currentUser().map(user -> user.id()).orElse(null),
                    com.medflow.common.security.SecurityUtils.currentUser().map(user -> user.realName()).orElse(null));
        } catch (RuntimeException auditFailure) {
            log.warn("failed to record access denied audit for {}", request.getRequestURI(), auditFailure);
        }
        return problemDetailsFactory.create(ErrorCode.AUTHZ_DENIED, request, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return problemDetailsFactory.create(ErrorCode.VERSION_CONFLICT, request, null);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest request) {
        log.warn("duplicate key path={}", request.getRequestURI());
        return problemDetailsFactory.create(ErrorCode.CONFLICT, request, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return problemDetailsFactory.create(ErrorCode.NOT_FOUND, request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return problemDetailsFactory.create(ErrorCode.METHOD_NOT_ALLOWED, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("unexpected error path={} requestId={}", request.getRequestURI(),
                com.medflow.common.web.RequestIdFilter.currentRequestId(request), ex);
        return problemDetailsFactory.create(ErrorCode.INTERNAL, request, null);
    }
}
