package com.medflow.common.error;

import java.util.List;
import java.util.Map;

/**
 * 可预期的业务失败。消息会直接呈现给用户，因此必须可读且不包含敏感信息。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<Map<String, String>> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultDetail(), List.of());
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, List.of());
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        this(errorCode, detail, List.of(), cause);
    }

    public BusinessException(ErrorCode errorCode, String detail, List<Map<String, String>> fieldErrors) {
        super(detail);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public BusinessException(ErrorCode errorCode, String detail, List<Map<String, String>> fieldErrors,
            Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<Map<String, String>> fieldErrors() {
        return fieldErrors;
    }
}
