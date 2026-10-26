package com.medflow.auth.domain;

import java.time.Duration;
public interface RefreshTokenStore {

    enum RotationStatus {
        ACTIVE, REPLAY, INVALID
    }

    record RotationResult(RotationStatus status, Long userId, String sessionId, String rawToken) {
    }

    IssuedRefreshToken issue(long userId, String sessionId);

    RotationResult rotate(String rawToken);

    void revoke(String rawToken);

    void revokeAll(long userId);

    void denyAccessToken(String jti, Duration ttl);

    boolean isAccessTokenDenied(String jti);

}
