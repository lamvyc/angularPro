package com.medflow.iam.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class SysRolePermissionPO {
    private Long roleId;
    private Long permissionId;
}
