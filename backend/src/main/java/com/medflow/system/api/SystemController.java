package com.medflow.system.api;

import com.medflow.system.api.dto.PingResponse;
import com.medflow.system.api.dto.SystemInfoResponse;
import com.medflow.system.application.SystemAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "系统信息与存活探测")
public class SystemController {

    private final SystemAppService systemAppService;

    public SystemController(SystemAppService systemAppService) {
        this.systemAppService = systemAppService;
    }

    @GetMapping("/ping")
    @Operation(summary = "存活探测（公开）", description = "供前端与容器探针使用，不返回任何业务数据")
    public PingResponse ping() {
        return new PingResponse("pong", systemAppService.now());
    }

    @GetMapping("/info")
    @Operation(summary = "系统运行信息（需登录）")
    public SystemInfoResponse info() {
        return SystemInfoResponse.from(systemAppService.info());
    }
}
