package com.medflow.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import com.medflow.auth.domain.IssuedRefreshToken;
import com.medflow.auth.domain.RefreshTokenStore;
import com.medflow.common.security.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:user-sessions:";
    private static final String DENY_PREFIX = "auth:deny:";

    /**
     * 原子轮换：第一个请求把 ACTIVE 标记为 ROTATED；并发或重放请求看到 ROTATED 即视为重放。
     */
    private static final DefaultRedisScript<String> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if not value then
              return 'MISSING'
            end
            if string.sub(value, -7) == '|ACTIVE' then
              local base = string.sub(value, 1, -8)
              redis.call('SET', KEYS[1], base .. '|ROTATED', 'KEEPTTL')
              return 'ACTIVE|' .. base
            end
            if string.sub(value, -8) == '|ROTATED' then
              return 'ROTATED|' .. string.sub(value, 1, -9)
            end
            return 'MISSING'
            """, String.class);

    private final StringRedisTemplate redis;
    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RedisRefreshTokenStore(StringRedisTemplate redis, JwtProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public IssuedRefreshToken issue(long userId, String sessionId) {
        String rawToken = randomToken();
        String hash = sha256(rawToken);
        Duration ttl = properties.refreshTokenTtl();
        redis.opsForValue().set(refreshKey(hash), userId + "|" + sessionId + "|ACTIVE", ttl);
        redis.opsForValue().set(sessionKey(sessionId), userId + "|" + hash, ttl);
        redis.opsForSet().add(userSessionsKey(userId), sessionId);
        redis.expire(userSessionsKey(userId), ttl);
        return new IssuedRefreshToken(rawToken, sessionId);
    }

    @Override
    public RotationResult rotate(String rawToken) {
        String oldHash = sha256(rawToken);
        String result = redis.execute(ROTATE_SCRIPT, List.of(refreshKey(oldHash)));
        if (result == null || result.startsWith("MISSING")) {
            return new RotationResult(RotationStatus.INVALID, null, null, null);
        }
        if (result.startsWith("ROTATED|")) {
            SessionRef ref = parse(result.substring("ROTATED|".length()));
            if (ref != null) {
                revokeSession(ref.userId(), ref.sessionId());
            }
            log.warn("refresh token replay detected for user={}", ref == null ? null : ref.userId());
            return new RotationResult(RotationStatus.REPLAY, ref == null ? null : ref.userId(),
                    ref == null ? null : ref.sessionId(), null);
        }

        SessionRef ref = parse(result.substring("ACTIVE|".length()));
        if (ref == null) {
            return new RotationResult(RotationStatus.INVALID, null, null, null);
        }
        String newRawToken = randomToken();
        String newHash = sha256(newRawToken);
        Duration ttl = properties.refreshTokenTtl();
        redis.opsForValue().set(refreshKey(newHash), ref.userId() + "|" + ref.sessionId() + "|ACTIVE", ttl);
        redis.opsForValue().set(sessionKey(ref.sessionId()), ref.userId() + "|" + newHash, ttl);
        return new RotationResult(RotationStatus.ACTIVE, ref.userId(), ref.sessionId(), newRawToken);
    }

    @Override
    public void revoke(String rawToken) {
        String hash = sha256(rawToken);
        String value = redis.opsForValue().get(refreshKey(hash));
        redis.delete(refreshKey(hash));
        SessionRef ref = value == null ? null : parse(value.replace("|ACTIVE", "").replace("|ROTATED", ""));
        if (ref != null) {
            revokeSession(ref.userId(), ref.sessionId());
        }
    }

    @Override
    public void revokeAll(long userId) {
        Set<String> sessionIds = redis.opsForSet().members(userSessionsKey(userId));
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                String value = redis.opsForValue().get(sessionKey(sessionId));
                if (value != null) {
                    String[] parts = value.split("\\|", 2);
                    if (parts.length == 2) {
                        redis.delete(refreshKey(parts[1]));
                    }
                }
                redis.delete(sessionKey(sessionId));
            }
        }
        redis.delete(userSessionsKey(userId));
    }

    @Override
    public void denyAccessToken(String jti, Duration ttl) {
        if (jti != null && !ttl.isZero() && !ttl.isNegative()) {
            redis.opsForValue().set(DENY_PREFIX + jti, "1", ttl);
        }
    }

    @Override
    public boolean isAccessTokenDenied(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(DENY_PREFIX + jti));
    }

    private void revokeSession(long userId, String sessionId) {
        String value = redis.opsForValue().get(sessionKey(sessionId));
        if (value != null) {
            String[] parts = value.split("\\|", 2);
            if (parts.length == 2) {
                redis.delete(refreshKey(parts[1]));
            }
        }
        redis.delete(sessionKey(sessionId));
        redis.opsForSet().remove(userSessionsKey(userId), sessionId);
    }

    private SessionRef parse(String value) {
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new SessionRef(Long.parseLong(parts[0]), parts[1]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String refreshKey(String hash) {
        return REFRESH_PREFIX + hash;
    }

    private String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    private String userSessionsKey(long userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    private record SessionRef(long userId, String sessionId) {
    }
}
