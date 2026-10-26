package com.medflow.iam.api;

import java.util.List;

import com.medflow.iam.application.StaffAppService;
import com.medflow.iam.domain.DoctorView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@Tag(name = "Staff", description = "科室人员（受数据范围约束）")
@RequiredArgsConstructor
public class StaffController {

    private final StaffAppService staffAppService;

    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('schedule:read')")
    @Operation(summary = "查询医生列表（按数据范围过滤）")
    public List<DoctorView> doctors(@RequestParam(required = false) Long deptId) {
        return staffAppService.doctors(deptId);
    }
}
