package com.medflow.iam.infrastructure.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_audit_log")
public class SysAuditLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private Long actorId;
    private String actorName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String result;
    private String errorCode;
    private String beforeJson;
    private String afterJson;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
