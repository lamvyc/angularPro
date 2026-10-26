package com.medflow.iam.api;

import java.util.List;

import com.medflow.iam.api.dto.AssignPermissionsRequest;
import com.medflow.iam.api.dto.CreateRoleRequest;
import com.medflow.iam.api.dto.PermissionIdsResponse;
import com.medflow.iam.api.dto.UpdateRoleRequest;
import com.medflow.iam.application.CreateRoleCommand;
import com.medflow.iam.application.RoleAppService;
import com.medflow.iam.application.UpdateRoleCommand;
import com.medflow.iam.domain.RoleView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role", description = "角色与授权")
@RequiredArgsConstructor
public class RoleController {

    private final RoleAppService roleAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:read')")
    @Operation(summary = "角色列表（含权限 ID）")
    public List<RoleView> list() {
        return roleAppService.list();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:read')")
    @Operation(summary = "角色权限 ID")
    public PermissionIdsResponse permissions(@PathVariable long id) {
        return new PermissionIdsResponse(roleAppService.permissionIds(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('system:role:create')")
    @Operation(summary = "新建角色")
    public RoleView create(@Valid @RequestBody CreateRoleRequest request) {
        return roleAppService.create(new CreateRoleCommand(
                request.code(), request.name(), request.dataScope(), request.remark()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    @Operation(summary = "编辑角色")
    public RoleView update(@PathVariable long id, @Valid @RequestBody UpdateRoleRequest request) {
        return roleAppService.update(id, new UpdateRoleCommand(
                request.name(), request.dataScope(), request.remark()));
    }

    @PutMapping("/{id}/permissions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('system:role:assign-permission')")
    @Operation(summary = "覆盖式分配权限并立即失效相关用户缓存")
    public void assignPermissions(@PathVariable long id, @Valid @RequestBody AssignPermissionsRequest request) {
        roleAppService.assignPermissions(id, request.permissionIds());
    }
}
