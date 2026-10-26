package com.medflow.auth.api.dto;

import com.medflow.auth.domain.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应")
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserProfile user) {
}
