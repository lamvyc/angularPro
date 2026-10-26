package com.medflow.iam.api;

import java.util.List;

import com.medflow.iam.api.dto.CreateDepartmentRequest;
import com.medflow.iam.api.dto.UpdateDepartmentRequest;
import com.medflow.iam.application.DepartmentAppService;
import com.medflow.iam.domain.DepartmentInfo;
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
@RequestMapping("/api/v1/departments")
@Tag(name = "Department", description = "科室管理")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentAppService departmentAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:dept:read')")
    @Operation(summary = "科室列表")
    public List<DepartmentInfo> list() {
        return departmentAppService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('system:dept:manage')")
    @Operation(summary = "新建科室")
    public DepartmentInfo create(@Valid @RequestBody CreateDepartmentRequest request) {
        return departmentAppService.create(request.code(), request.name(), request.parentId(),
                request.type(), request.sortNo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:manage')")
    @Operation(summary = "编辑科室（编码不可变）")
    public DepartmentInfo update(@PathVariable long id, @Valid @RequestBody UpdateDepartmentRequest request) {
        return departmentAppService.update(id, request.name(), request.parentId(), request.type(),
                request.sortNo());
    }
}
