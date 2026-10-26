package com.medflow.iam.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.medflow.common.web.PageResult;
import com.medflow.iam.domain.LoginState;
import com.medflow.iam.domain.NewUser;
import com.medflow.iam.domain.PermissionInfo;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.UserAccount;
import com.medflow.iam.domain.UserQuery;
import com.medflow.iam.domain.UserRepository;
import com.medflow.iam.domain.UserRow;
import com.medflow.iam.infrastructure.persistence.SysUserMapper;
import com.medflow.iam.infrastructure.persistence.SysUserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SysUserMapper mapper;

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return mapper.findAccountByUsername(username);
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return mapper.findAccountById(id);
    }

    @Override
    public List<RoleInfo> findRoles(long userId) {
        return mapper.findRolesByUserId(userId);
    }

    @Override
    public List<PermissionInfo> findPermissions(long userId) {
        return mapper.findPermissionsByUserId(userId);
    }

    @Override
    public List<PermissionInfo> findMenus(long userId) {
        return mapper.findMenusByUserId(userId);
    }

    @Override
    public List<UserAccount> findDoctors(Long deptId) {
        return mapper.findDoctors(deptId);
    }

    @Override
    public PageResult<UserRow> search(UserQuery query) {
        long offset = (long) (query.page() - 1) * query.size();
        List<UserRow> rows = mapper.searchUsers(query, offset, query.size());
        long total = mapper.countUsers(query);
        return PageResult.of(rows, query.page(), query.size(), total);
    }

    @Override
    public boolean existsByUsername(String username) {
        return mapper.countByUsername(username) > 0;
    }

    @Override
    public boolean existsByEmployeeNo(String employeeNo, Long excludeUserId) {
        if (employeeNo == null || employeeNo.isBlank()) {
            return false;
        }
        return mapper.countByEmployeeNo(employeeNo, excludeUserId) > 0;
    }

    @Override
    public long insert(NewUser user) {
        SysUserPO po = new SysUserPO();
        po.setUsername(user.username());
        po.setPasswordHash(user.passwordHash());
        po.setRealName(user.realName());
        po.setEmployeeNo(user.employeeNo());
        po.setDeptId(user.deptId());
        po.setTitle(user.title());
        po.setPhone(user.phone());
        po.setStatus("ENABLED");
        po.setFailedAttempts(0);
        mapper.insert(po);
        return po.getId();
    }

    @Override
    public void updateProfile(long id, String realName, Long deptId, String title, String phone,
            Long operatorId) {
        mapper.updateProfile(id, realName, deptId, title, phone, operatorId);
    }

    @Override
    public void replaceRoles(long userId, List<Long> roleIds) {
        mapper.deleteRoles(userId);
        if (!roleIds.isEmpty()) {
            mapper.insertRoles(userId, roleIds);
        }
    }

    @Override
    public void updateStatus(long id, String status, Long operatorId) {
        mapper.updateStatus(id, status, operatorId);
    }

    @Override
    public void updatePassword(long id, String passwordHash, Instant changedAt, Long operatorId) {
        mapper.updatePassword(id, passwordHash, changedAt, operatorId);
    }

    @Override
    public void recordLoginSuccess(long id, Instant at) {
        mapper.recordLoginSuccess(id, at);
    }

    @Override
    public LoginState recordLoginFailure(long id, int maxAttempts, Instant lockedUntil) {
        mapper.incrementFailure(id, maxAttempts, lockedUntil);
        return mapper.findLoginState(id);
    }

    @Override
    public void resetLoginState(long id) {
        mapper.resetLoginState(id);
    }
}
