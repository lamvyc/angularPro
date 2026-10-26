package com.medflow.iam.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medflow.iam.domain.LoginState;
import com.medflow.iam.domain.PermissionInfo;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.UserAccount;
import com.medflow.iam.domain.UserQuery;
import com.medflow.iam.domain.UserRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {

    @Select("""
            SELECT u.id, u.username, u.password_hash AS passwordHash, u.real_name AS realName,
                   u.employee_no AS employeeNo, u.dept_id AS deptId, d.name AS deptName,
                   u.title, u.phone, u.status, u.failed_attempts AS failedAttempts,
                   u.locked_until AS lockedUntil, u.last_login_at AS lastLoginAt, u.version
            FROM sys_user u
            JOIN sys_department d ON d.id = u.dept_id
            WHERE u.username = #{username}
            """)
    Optional<UserAccount> findAccountByUsername(String username);

    @Select("""
            SELECT u.id, u.username, u.password_hash AS passwordHash, u.real_name AS realName,
                   u.employee_no AS employeeNo, u.dept_id AS deptId, d.name AS deptName,
                   u.title, u.phone, u.status, u.failed_attempts AS failedAttempts,
                   u.locked_until AS lockedUntil, u.last_login_at AS lastLoginAt, u.version
            FROM sys_user u
            JOIN sys_department d ON d.id = u.dept_id
            WHERE u.id = #{id}
            """)
    Optional<UserAccount> findAccountById(long id);

    @Select("""
            SELECT r.id, r.code, r.name, r.data_scope AS dataScope, r.built_in AS builtIn,
                   r.status, r.remark
            FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.id
            """)
    List<RoleInfo> findRolesByUserId(long userId);

    @Select("""
            SELECT DISTINCT p.id, p.code, p.name, p.type, p.parent_id AS parentId,
                            p.route_path AS routePath, p.icon, p.sort_no AS sortNo
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 'ENABLED'
            WHERE ur.user_id = #{userId} AND p.status = 'ENABLED'
            ORDER BY p.sort_no, p.id
            """)
    List<PermissionInfo> findPermissionsByUserId(long userId);

    @Select("""
            SELECT DISTINCT p.id, p.code, p.name, p.type, p.parent_id AS parentId,
                            p.route_path AS routePath, p.icon, p.sort_no AS sortNo
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 'ENABLED'
            WHERE ur.user_id = #{userId} AND p.status = 'ENABLED' AND p.type = 'MENU'
            ORDER BY p.sort_no, p.id
            """)
    List<PermissionInfo> findMenusByUserId(long userId);

    @Select("""
            <script>
            SELECT u.id, u.username, u.password_hash AS passwordHash, u.real_name AS realName,
                   u.employee_no AS employeeNo, u.dept_id AS deptId, d.name AS deptName,
                   u.title, u.phone, u.status, u.failed_attempts AS failedAttempts,
                   u.locked_until AS lockedUntil, u.last_login_at AS lastLoginAt, u.version
            FROM sys_user u
            JOIN sys_department d ON d.id = u.dept_id
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE r.code = 'DOCTOR' AND u.status = 'ENABLED'
            <if test="deptId != null">AND u.dept_id = #{deptId}</if>
            ORDER BY u.id
            </script>
            """)
    List<UserAccount> findDoctors(@Param("deptId") Long deptId);

    @Select("""
            <script>
            SELECT u.id, u.username, u.real_name AS realName, u.employee_no AS employeeNo,
                   u.dept_id AS deptId, d.name AS deptName, u.title, u.phone, u.status,
                   u.last_login_at AS lastLoginAt,
                   GROUP_CONCAT(DISTINCT r.id ORDER BY r.id) AS roleIds,
                   GROUP_CONCAT(DISTINCT r.code ORDER BY r.id) AS roleCodes,
                   GROUP_CONCAT(DISTINCT r.name ORDER BY r.id) AS roleNames
            FROM sys_user u
            JOIN sys_department d ON d.id = u.dept_id
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id
            <where>
              <if test="q.keyword != null and q.keyword != ''">
                AND (u.username LIKE CONCAT('%', #{q.keyword}, '%')
                  OR u.real_name LIKE CONCAT('%', #{q.keyword}, '%')
                  OR u.employee_no LIKE CONCAT('%', #{q.keyword}, '%'))
              </if>
              <if test="q.deptId != null">AND u.dept_id = #{q.deptId}</if>
              <if test="q.status != null and q.status != ''">AND u.status = #{q.status}</if>
            </where>
            GROUP BY u.id, d.name
            ORDER BY u.id
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<UserRow> searchUsers(@Param("q") UserQuery query, @Param("offset") long offset,
            @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM sys_user u
            <where>
              <if test="q.keyword != null and q.keyword != ''">
                AND (u.username LIKE CONCAT('%', #{q.keyword}, '%')
                  OR u.real_name LIKE CONCAT('%', #{q.keyword}, '%')
                  OR u.employee_no LIKE CONCAT('%', #{q.keyword}, '%'))
              </if>
              <if test="q.deptId != null">AND u.dept_id = #{q.deptId}</if>
              <if test="q.status != null and q.status != ''">AND u.status = #{q.status}</if>
            </where>
            </script>
            """)
    long countUsers(@Param("q") UserQuery query);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username}")
    int countByUsername(String username);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user
            WHERE employee_no = #{employeeNo}
            <if test="excludeUserId != null">AND id &lt;&gt; #{excludeUserId}</if>
            </script>
            """)
    int countByEmployeeNo(@Param("employeeNo") String employeeNo,
            @Param("excludeUserId") Long excludeUserId);

    @Update("""
            UPDATE sys_user
            SET real_name = #{realName}, dept_id = #{deptId}, title = #{title}, phone = #{phone},
                updated_by = #{operatorId}, updated_at = NOW(3)
            WHERE id = #{id}
            """)
    int updateProfile(@Param("id") long id, @Param("realName") String realName,
            @Param("deptId") Long deptId, @Param("title") String title, @Param("phone") String phone,
            @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE sys_user
            SET status = #{status},
                failed_attempts = CASE WHEN #{status} = 'ENABLED' THEN 0 ELSE failed_attempts END,
                locked_until = CASE WHEN #{status} = 'ENABLED' THEN NULL ELSE locked_until END,
                updated_by = #{operatorId}, updated_at = NOW(3)
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") long id, @Param("status") String status,
            @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE sys_user
            SET password_hash = #{passwordHash}, password_changed_at = #{changedAt},
                failed_attempts = 0, locked_until = NULL, updated_by = #{operatorId}, updated_at = NOW(3)
            WHERE id = #{id}
            """)
    int updatePassword(@Param("id") long id, @Param("passwordHash") String passwordHash,
            @Param("changedAt") Instant changedAt, @Param("operatorId") Long operatorId);

    @Update("""
            UPDATE sys_user
            SET failed_attempts = 0, last_login_at = #{at}, updated_at = NOW(3),
                status = CASE WHEN status = 'LOCKED' THEN 'ENABLED' ELSE status END,
                locked_until = NULL
            WHERE id = #{id}
            """)
    int recordLoginSuccess(@Param("id") long id, @Param("at") Instant at);

    @Update("""
            UPDATE sys_user
            SET status = CASE WHEN failed_attempts + 1 >= #{maxAttempts} THEN 'LOCKED' ELSE status END,
                locked_until = CASE WHEN failed_attempts + 1 >= #{maxAttempts}
                                    THEN #{lockedUntil}
                                    ELSE locked_until END,
                failed_attempts = failed_attempts + 1,
                updated_at = NOW(3)
            WHERE id = #{id}
            """)
    int incrementFailure(@Param("id") long id, @Param("maxAttempts") int maxAttempts,
            @Param("lockedUntil") Instant lockedUntil);

    @Select("""
            SELECT failed_attempts AS failedAttempts, status, locked_until AS lockedUntil
            FROM sys_user WHERE id = #{id}
            """)
    LoginState findLoginState(long id);

    @Update("""
            UPDATE sys_user SET failed_attempts = 0, locked_until = NULL,
                status = CASE WHEN status = 'LOCKED' THEN 'ENABLED' ELSE status END,
                updated_at = NOW(3)
            WHERE id = #{id}
            """)
    int resetLoginState(long id);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteRoles(long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection="roleIds" item="roleId" separator=",">
              (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertRoles(@Param("userId") long userId, @Param("roleIds") List<Long> roleIds);

    @Select("SELECT ur.user_id FROM sys_user_role ur WHERE ur.role_id = #{roleId}")
    List<Long> findUserIdsByRoleId(long roleId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId}")
    long countUsersByDept(long deptId);
}
