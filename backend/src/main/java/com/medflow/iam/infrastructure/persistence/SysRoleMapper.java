package com.medflow.iam.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medflow.iam.domain.RoleInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePO> {

    @Select("""
            SELECT id, code, name, data_scope AS dataScope, built_in AS builtIn, status, remark
            FROM sys_role ORDER BY id
            """)
    List<RoleInfo> findAllRoles();

    @Select("""
            SELECT id, code, name, data_scope AS dataScope, built_in AS builtIn, status, remark
            FROM sys_role WHERE id = #{id}
            """)
    Optional<RoleInfo> findRole(long id);

    @Select("SELECT COUNT(*) FROM sys_role WHERE code = #{code}")
    int countByCode(String code);

    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} ORDER BY permission_id")
    List<Long> findPermissionIds(long roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deletePermissions(long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
              (#{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    int insertPermissions(@Param("roleId") long roleId, @Param("permissionIds") List<Long> permissionIds);
}
