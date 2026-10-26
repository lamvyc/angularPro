package com.medflow.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medflow.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String secret) {

    public JwtProperties {
        issuer = issuer == null || issuer.isBlank() ? "medflow" : issuer;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
    }

    public byte[] secretBytes() {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "medflow.jwt.secret 未配置或长度不足 32 字节；请通过 JWT_SECRET 环境变量注入");
        }
        return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
