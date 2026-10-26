package com.medflow.iam.application;

import java.util.List;
import java.util.Map;

import com.medflow.auth.application.PrincipalService;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.DataScope;
import com.medflow.iam.domain.PermissionRepository;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.RoleRepository;
import com.medflow.iam.domain.RoleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleAppService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PrincipalService principalService;
    private final AuditService auditService;

    public List<RoleView> list() {
        return roleRepository.findAll().stream().map(role -> toView(role)).toList();
    }

    public List<Long> permissionIds(long roleId) {
        requireRole(roleId);
        return roleRepository.findPermissionIds(roleId);
    }

    @Transactional
    public RoleView create(CreateRoleCommand command) {
        String code = command.code() == null ? "" : command.code().trim().toUpperCase();
        if (!code.matches("[A-Z][A-Z0-9_]{2,31}")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "角色编码需为 3-32 位大写字母、数字或下划线");
        }
        if (roleRepository.existsByCode(code)) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        long roleId = roleRepository.insert(code, command.name().trim(), command.dataScope(),
                command.remark());
        auditService.success("ROLE_CREATED", "ROLE", String.valueOf(roleId), null,
                Map.of("code", code, "name", command.name(), "dataScope", command.dataScope().name()));
        return toView(requireRole(roleId));
    }

    @Transactional
    public RoleView update(long roleId, UpdateRoleCommand command) {
        RoleInfo before = requireRole(roleId);
        if ("ADMIN".equals(before.code()) && command.dataScope() != DataScope.ALL) {
            throw new BusinessException(ErrorCode.IAM_PROTECTED, "内置管理员角色的数据范围必须保持 ALL");
        }
        roleRepository.update(roleId, command.name().trim(), command.dataScope(), command.remark());
        roleRepository.findUserIds(roleId).forEach(principalService::evict);
        auditService.success("ROLE_UPDATED", "ROLE", String.valueOf(roleId),
                Map.of("name", before.name(), "dataScope", before.dataScope().name()),
                Map.of("name", command.name(), "dataScope", command.dataScope().name()));
        return toView(requireRole(roleId));
    }

    @Transactional
    public void assignPermissions(long roleId, List<Long> permissionIds) {
        RoleInfo role = requireRole(roleId);
        if ("ADMIN".equals(role.code())) {
            throw new BusinessException(ErrorCode.IAM_PROTECTED, "内置管理员角色的权限不允许修改");
        }
        List<Long> distinct = permissionIds == null ? List.of() : permissionIds.stream().distinct().toList();
        if (!distinct.isEmpty() && permissionRepository.findByIds(distinct).size() != distinct.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "存在无效的权限 ID");
        }
        List<Long> before = roleRepository.findPermissionIds(roleId);
        roleRepository.replacePermissions(roleId, distinct);
        roleRepository.findUserIds(roleId).forEach(principalService::evict);
        auditService.success("ROLE_PERMISSION_CHANGED", "ROLE", String.valueOf(roleId),
                Map.of("permissionIds", before), Map.of("permissionIds", distinct));
    }

    private RoleView toView(RoleInfo role) {
        return new RoleView(role.id(), role.code(), role.name(), role.dataScope(), role.builtIn(),
                role.status(), role.remark(), roleRepository.findPermissionIds(role.id()));
    }

    private RoleInfo requireRole(long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
    }
}
