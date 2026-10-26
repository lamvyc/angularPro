package com.medflow.common.error;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码。HTTP 状态语义与业务码分离，前端按 code 分支处理。
 */
public enum ErrorCode {

    AUTH_INVALID(HttpStatus.UNAUTHORIZED, "MED-AUTH-001", "登录凭据无效", "用户名或密码错误"),
    AUTH_LOCKED(HttpStatus.LOCKED, "MED-AUTH-002", "账号已锁定", "登录失败次数过多，请稍后再试"),
    AUTHZ_DENIED(HttpStatus.FORBIDDEN, "MED-AUTHZ-001", "无访问权限", "没有执行该操作的权限"),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "MED-AUTH-003", "会话已失效", "刷新令牌无效或已被使用"),
    AUTH_USER_DISABLED(HttpStatus.FORBIDDEN, "MED-AUTH-004", "账号已停用", "账号已被停用，请联系管理员"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "MED-NOT-FOUND", "资源不存在", "资源不存在或已被删除"),
    STATE_CONFLICT(HttpStatus.CONFLICT, "MED-STATE-001", "状态冲突", "当前状态不允许该操作"),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "MED-STATE-002", "数据已被修改", "数据已被其他用户修改，请刷新后重试"),
    SLOT_FULL(HttpStatus.CONFLICT, "MED-SLOT-001", "号源已满", "该号源已被约满"),
    SLOT_DUPLICATE(HttpStatus.CONFLICT, "MED-SLOT-002", "重复预约", "患者已有该号源的有效预约"),
    STOCK_INSUFFICIENT(HttpStatus.CONFLICT, "MED-STOCK-001", "库存不足", "药品库存不足"),
    IDEMPOTENT_IN_PROGRESS(HttpStatus.CONFLICT, "MED-IDEM-001", "请求处理中", "相同请求正在处理，请稍后重试"),
    EXPIRED(HttpStatus.GONE, "MED-EXPIRED-001", "已过期", "号源或处方已过期"),
    CONFLICT(HttpStatus.CONFLICT, "MED-CONFLICT-001", "数据冲突", "数据冲突，请检查后重试"),
    IAM_PROTECTED(HttpStatus.CONFLICT, "MED-IAM-001", "受保护的数据", "内置角色或当前账号不允许该操作"),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "MED-VALID-001", "参数校验失败", "请求参数不合法"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "MED-METHOD-001", "方法不支持", "请求方法不被支持"),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "MED-INTERNAL", "系统错误", "系统繁忙，请稍后重试");

    private final HttpStatus status;
    private final String code;
    private final String title;
    private final String defaultDetail;

    ErrorCode(HttpStatus status, String code, String title, String defaultDetail) {
        this.status = status;
        this.code = code;
        this.title = title;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String defaultDetail() {
        return defaultDetail;
    }
}
