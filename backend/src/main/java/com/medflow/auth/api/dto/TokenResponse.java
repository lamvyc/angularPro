package com.medflow.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "刷新令牌响应")
public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType) {
}
