package com.medflow.iam.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.medflow.auth.application.PrincipalService;
import com.medflow.auth.domain.RefreshTokenStore;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.common.web.PageResult;
import com.medflow.iam.domain.DepartmentInfo;
import com.medflow.iam.domain.DepartmentRepository;
import com.medflow.iam.domain.NewUser;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.RoleRepository;
import com.medflow.iam.domain.UserAccount;
import com.medflow.iam.domain.UserQuery;
import com.medflow.iam.domain.UserRepository;
import com.medflow.iam.domain.UserRow;
import com.medflow.iam.domain.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final PrincipalService principalService;
    private final AuditService auditService;
    private final Clock clock;

    public PageResult<UserView> search(int page, int size, String keyword, Long deptId, String status) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        PageResult<UserRow> rows = userRepository.search(
                new UserQuery(safePage, safeSize, normalize(keyword), deptId, normalize(status)));
        List<UserView> views = rows.items().stream().map(this::toView).toList();
        return PageResult.of(views, rows.page(), rows.size(), rows.total());
    }

    @Transactional
    public UserView create(CreateUserCommand command) {
        String username = normalize(command.username());
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "用户名需为 3-50 位字母、数字、点、下划线或短横线");
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (userRepository.existsByEmployeeNo(normalize(command.employeeNo()), null)) {
            throw new BusinessException(ErrorCode.CONFLICT, "工号已存在");
        }
        DepartmentInfo department = requireDepartment(command.deptId());
        List<Long> roleIds = requireRoles(command.roleIds());

        long userId = userRepository.insert(new NewUser(
                username,
                passwordEncoder.encode(command.password()),
                normalize(command.realName()),
                normalize(command.employeeNo()),
                department.id(),
                normalize(command.title()),
                normalize(command.phone())));
        userRepository.replaceRoles(userId, roleIds);
        principalService.evict(userId);

        auditService.success("USER_CREATED", "USER", String.valueOf(userId), null,
                mapOf("username", username, "deptId", department.id(), "roleIds", roleIds));
        return view(userId);
    }

    @Transactional
    public UserView update(long userId, UpdateUserCommand command) {
        UserAccount before = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        DepartmentInfo department = requireDepartment(command.deptId());
        List<Long> roleIds = requireRoles(command.roleIds());

        userRepository.updateProfile(userId, normalize(command.realName()), department.id(),
                normalize(command.title()), normalize(command.phone()), currentUserId());
        userRepository.replaceRoles(userId, roleIds);
        principalService.evict(userId);
        auditService.success("USER_UPDATED", "USER", String.valueOf(userId),
                mapOf("realName", before.realName(), "deptId", before.deptId()),
                mapOf("realName", command.realName(), "deptId", department.id(), "roleIds", roleIds));
        return view(userId);
    }

    @Transactional
    public void changeStatus(long userId, String status) {
        if (!Set.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "状态只能是 ENABLED 或 DISABLED");
        }
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        if (currentUser.id() == userId) {
            throw new BusinessException(ErrorCode.IAM_PROTECTED, "不能停用或锁定当前登录账号");
        }
        UserAccount before = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        userRepository.updateStatus(userId, status, currentUser.id());
        principalService.evict(userId);
        if ("DISABLED".equals(status)) {
            refreshTokenStore.revokeAll(userId);
        }
        auditService.success("USER_STATUS_CHANGED", "USER", String.valueOf(userId),
                mapOf("status", before.status()), mapOf("status", status));
    }

    @Transactional
    public void resetPassword(long userId, String newPassword) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword), Instant.now(clock),
                currentUserId());
        principalService.evict(userId);
        refreshTokenStore.revokeAll(userId);
        auditService.success("USER_PASSWORD_RESET", "USER", String.valueOf(userId), null,
                mapOf("username", user.username()));
    }

    private UserView view(long userId) {
        UserAccount account = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
        List<UserView.RoleRef> roles = userRepository.findRoles(userId).stream()
                .map(role -> new UserView.RoleRef(role.id(), role.code(), role.name()))
                .toList();
        return new UserView(account.id(), account.username(), account.realName(), account.employeeNo(),
                account.deptId(), account.deptName(), account.title(), account.phone(), account.status(),
                account.lastLoginAt(), roles);
    }

    private UserView toView(UserRow row) {
        String[] ids = split(row.roleIds());
        String[] codes = split(row.roleCodes());
        String[] names = split(row.roleNames());
        List<UserView.RoleRef> roles = new ArrayList<>();
        for (int i = 0; i < ids.length && i < codes.length && i < names.length; i++) {
            try {
                roles.add(new UserView.RoleRef(Long.parseLong(ids[i]), codes[i], names[i]));
            } catch (NumberFormatException ignored) {
                // 聚合列异常时忽略该角色，避免整个列表失败
            }
        }
        return new UserView(row.id(), row.username(), row.realName(), row.employeeNo(), row.deptId(),
                row.deptName(), row.title(), row.phone(), row.status(), row.lastLoginAt(), roles);
    }

    private DepartmentInfo requireDepartment(Long deptId) {
        if (deptId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "科室不能为空");
        }
        DepartmentInfo department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "科室不存在"));
        if (!"ENABLED".equals(department.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "科室已停用");
        }
        return department;
    }

    private List<Long> requireRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "至少分配一个角色");
        }
        Map<Long, RoleInfo> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleInfo::id, role -> role));
        List<Long> distinct = roleIds.stream().distinct().toList();
        for (Long roleId : distinct) {
            RoleInfo role = roles.get(roleId);
            if (role == null || !role.enabled()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色不存在或已停用: " + roleId);
            }
        }
        return distinct;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUser().map(CurrentUser::id).orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String[] split(String value) {
        return value == null || value.isBlank() ? new String[0] : value.split(",");
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
