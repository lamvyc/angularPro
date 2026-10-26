package com.medflow.iam.api;

import java.util.List;

import com.medflow.iam.application.PermissionAppService;
import com.medflow.iam.domain.PermissionNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permission", description = "权限树")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionAppService permissionAppService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:role:read')")
    @Operation(summary = "权限树（菜单/按钮/接口）")
    public List<PermissionNode> tree() {
        return permissionAppService.tree();
    }
}
