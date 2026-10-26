package com.medflow.system.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "存活探测响应")
public record PingResponse(
        @Schema(example = "pong") String message,
        Instant serverTime) {
}
