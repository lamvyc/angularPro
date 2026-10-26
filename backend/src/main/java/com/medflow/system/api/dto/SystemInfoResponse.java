package com.medflow.system.api.dto;

import java.time.Instant;
import java.util.List;

import com.medflow.system.domain.SystemInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统信息（需登录）")
public record SystemInfoResponse(String application, String version, List<String> profiles, Instant serverTime) {

    public static SystemInfoResponse from(SystemInfo info) {
        return new SystemInfoResponse(info.application(), info.version(), info.profiles(), info.serverTime());
    }
}
