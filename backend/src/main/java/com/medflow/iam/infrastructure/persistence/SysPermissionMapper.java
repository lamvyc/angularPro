package com.medflow.iam.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medflow.iam.domain.PermissionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionPO> {

    @Select("""
            SELECT id, code, name, type, parent_id AS parentId, route_path AS routePath,
                   icon, sort_no AS sortNo
            FROM sys_permission WHERE status = 'ENABLED'
            ORDER BY sort_no, id
            """)
    List<PermissionInfo> findAllPermissions();

    @Select("""
            <script>
            SELECT id, code, name, type, parent_id AS parentId, route_path AS routePath,
                   icon, sort_no AS sortNo
            FROM sys_permission
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<PermissionInfo> findPermissionsByIds(@Param("ids") Collection<Long> ids);
}
