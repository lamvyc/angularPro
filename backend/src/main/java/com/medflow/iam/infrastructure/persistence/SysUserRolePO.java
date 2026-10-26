package com.medflow.iam.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRolePO {
    private Long userId;
    private Long roleId;
}
