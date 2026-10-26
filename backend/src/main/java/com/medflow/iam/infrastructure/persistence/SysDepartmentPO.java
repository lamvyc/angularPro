package com.medflow.iam.infrastructure.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_department")
public class SysDepartmentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String type;
    private String status;
    private Integer sortNo;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Integer version;
}
