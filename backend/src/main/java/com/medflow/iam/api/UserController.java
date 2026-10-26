package com.medflow.iam.api;

import com.medflow.common.web.PageResult;
import com.medflow.iam.api.dto.CreateUserRequest;
import com.medflow.iam.api.dto.ResetPasswordRequest;
import com.medflow.iam.api.dto.UpdateUserRequest;
import com.medflow.iam.api.dto.UserStatusRequest;
import com.medflow.iam.application.CreateUserCommand;
import com.medflow.iam.application.UpdateUserCommand;
import com.medflow.iam.application.UserAppService;
import com.medflow.iam.domain.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "用户管理")
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:read')")
    @Operation(summary = "分页查询用户")
    public PageResult<UserView> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status) {
        return userAppService.search(page, size, keyword, deptId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('system:user:create')")
    @Operation(summary = "新建用户")
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return userAppService.create(new CreateUserCommand(
                request.username(), request.password(), request.realName(), request.employeeNo(),
                request.deptId(), request.title(), request.phone(), request.roleIds()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    @Operation(summary = "编辑用户")
    public UserView update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        return userAppService.update(id, new UpdateUserCommand(
                request.realName(), request.deptId(), request.title(), request.phone(), request.roleIds()));
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('system:user:disable')")
    @Operation(summary = "启用/停用用户，同时解除锁定")
    public void changeStatus(@PathVariable long id, @Valid @RequestBody UserStatusRequest request) {
        userAppService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('system:user:reset-password')")
    @Operation(summary = "重置密码并吊销该用户全部刷新令牌")
    public void resetPassword(@PathVariable long id, @Valid @RequestBody ResetPasswordRequest request) {
        userAppService.resetPassword(id, request.newPassword());
    }
}
