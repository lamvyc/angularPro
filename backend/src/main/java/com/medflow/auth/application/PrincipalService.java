package com.medflow.auth.application;

import java.util.List;
import java.util.Optional;

import com.medflow.auth.domain.AuthorityCache;
import com.medflow.auth.domain.AuthoritySnapshot;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.DataScope;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.UserAccount;
import com.medflow.iam.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 权限快照的加载与缓存。角色授权或用户资料变更后由 IAM 服务调用 evict。
 */
@Service
@RequiredArgsConstructor
public class PrincipalService {

    private final UserRepository userRepository;
    private final AuthorityCache authorityCache;

    public Optional<AuthoritySnapshot> load(long userId) {
        Optional<AuthoritySnapshot> cached = authorityCache.get(userId);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<UserAccount> account = userRepository.findById(userId);
        if (account.isEmpty() || !"ENABLED".equals(account.get().status())) {
            return Optional.empty();
        }
        UserAccount user = account.get();
        List<RoleInfo> roles = userRepository.findRoles(userId);
        List<String> permissions = userRepository.findPermissions(userId).stream()
                .map(permission -> permission.code())
                .toList();
        DataScope dataScope = DataScope.strongest(roles.stream().map(RoleInfo::dataScope).toList());
        AuthoritySnapshot snapshot = new AuthoritySnapshot(
                user.id(), user.username(), user.realName(), user.deptId(), user.deptName(), user.title(),
                user.status(), dataScope, roles, permissions, userRepository.findMenus(userId));
        authorityCache.put(userId, snapshot);
        return Optional.of(snapshot);
    }

    public Optional<CurrentUser> loadCurrentUser(long userId) {
        return load(userId).map(this::toCurrentUser);
    }

    public CurrentUser toCurrentUser(AuthoritySnapshot snapshot) {
        return new CurrentUser(
                snapshot.userId(),
                snapshot.username(),
                snapshot.realName(),
                snapshot.deptId(),
                snapshot.deptName(),
                snapshot.title(),
                snapshot.dataScope(),
                snapshot.roles().stream().map(RoleInfo::code).toList(),
                snapshot.permissions());
    }

    public void evict(long userId) {
        authorityCache.evict(userId);
    }
}
