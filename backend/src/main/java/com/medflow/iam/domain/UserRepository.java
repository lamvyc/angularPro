package com.medflow.iam.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.medflow.common.web.PageResult;

public interface UserRepository {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(long id);

    List<RoleInfo> findRoles(long userId);

    List<PermissionInfo> findPermissions(long userId);

    List<PermissionInfo> findMenus(long userId);

    List<UserAccount> findDoctors(Long deptId);

    PageResult<UserRow> search(UserQuery query);

    boolean existsByUsername(String username);

    boolean existsByEmployeeNo(String employeeNo, Long excludeUserId);

    long insert(NewUser user);

    void updateProfile(long id, String realName, Long deptId, String title, String phone, Long operatorId);

    void replaceRoles(long userId, List<Long> roleIds);

    void updateStatus(long id, String status, Long operatorId);

    void updatePassword(long id, String passwordHash, Instant changedAt, Long operatorId);

    void recordLoginSuccess(long id, Instant at);

    LoginState recordLoginFailure(long id, int maxAttempts, Instant lockedUntil);

    void resetLoginState(long id);
}
