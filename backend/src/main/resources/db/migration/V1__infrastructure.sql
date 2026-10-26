-- MedFlow V1: infrastructure tables shared by all business modules.
-- MySQL 8.4 LTS / utf8mb4_0900_ai_ci / InnoDB

CREATE TABLE sys_audit_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id      VARCHAR(64)     NULL COMMENT '请求链路 ID',
    actor_id      BIGINT UNSIGNED NULL COMMENT '操作人 ID（系统任务为空）',
    actor_name    VARCHAR(50)     NULL COMMENT '操作人姓名快照',
    action        VARCHAR(64)     NOT NULL COMMENT '业务动作，如 EMR_SIGNED',
    resource_type VARCHAR(32)     NOT NULL COMMENT '资源类型',
    resource_id   VARCHAR(64)     NULL COMMENT '资源 ID',
    result        VARCHAR(16)     NOT NULL COMMENT 'SUCCESS / FAILURE',
    error_code    VARCHAR(32)     NULL COMMENT '失败错误码',
    before_json   JSON            NULL COMMENT '变更前快照（脱敏）',
    after_json    JSON            NULL COMMENT '变更后快照（脱敏）',
    ip            VARCHAR(45)     NULL COMMENT '来源 IP',
    user_agent    VARCHAR(255)    NULL COMMENT 'User-Agent',
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_audit_resource (resource_type, resource_id, created_at),
    KEY idx_audit_actor (actor_id, created_at),
    KEY idx_audit_created (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '审计日志（只追加）';

CREATE TABLE sys_idempotency_record (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    idem_key        VARCHAR(80)     NOT NULL COMMENT '客户端幂等键',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '作用域用户',
    method          VARCHAR(10)     NOT NULL COMMENT 'HTTP 方法',
    path            VARCHAR(255)    NOT NULL COMMENT '请求路径',
    request_hash    CHAR(64)        NOT NULL COMMENT '请求体 SHA-256',
    status          VARCHAR(16)     NOT NULL COMMENT 'IN_PROGRESS / COMPLETED / FAILED',
    response_status INT             NULL COMMENT '首次响应 HTTP 状态',
    response_body   JSON            NULL COMMENT '首次响应快照',
    error_code      VARCHAR(32)     NULL COMMENT '失败错误码',
    expires_at      DATETIME(3)     NOT NULL COMMENT '过期时间（默认 24h）',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idem_user_key (user_id, idem_key),
    KEY idx_idem_expires (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '写操作幂等记录';

CREATE TABLE sys_sequence (
    name       VARCHAR(64) NOT NULL COMMENT '序列名，按日携带日期',
    next_value BIGINT      NOT NULL COMMENT '下一个值',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务编号序列';

CREATE TABLE outbox_event (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_type     VARCHAR(64)     NOT NULL COMMENT '事件类型',
    aggregate_type VARCHAR(32)     NOT NULL COMMENT '聚合类型',
    aggregate_id   VARCHAR(64)     NOT NULL COMMENT '聚合 ID',
    payload        JSON            NOT NULL COMMENT '事件内容',
    status         VARCHAR(16)     NOT NULL DEFAULT 'NEW' COMMENT 'NEW / PUBLISHED / FAILED',
    retry_count    INT             NOT NULL DEFAULT 0,
    next_retry_at  DATETIME(3)     NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at   DATETIME(3)     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_status (status, next_retry_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '事务性发件箱（外部副作用）';
