package com.medflow.auth.infrastructure;

import java.time.Duration;
import java.util.Optional;

import com.medflow.auth.domain.AuthorityCache;
import com.medflow.auth.domain.AuthoritySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAuthorityCache implements AuthorityCache {

    private static final String PREFIX = "perm:user:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    @Override
    public Optional<AuthoritySnapshot> get(long userId) {
        try {
            String value = redis.opsForValue().get(PREFIX + userId);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(jsonMapper.readValue(value, AuthoritySnapshot.class));
        } catch (RuntimeException ex) {
            log.warn("read authority cache failed for user={}, fallback to database", userId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(long userId, AuthoritySnapshot snapshot) {
        try {
            redis.opsForValue().set(PREFIX + userId, jsonMapper.writeValueAsString(snapshot), TTL);
        } catch (RuntimeException ex) {
            log.warn("write authority cache failed for user={}", userId, ex);
        }
    }

    @Override
    public void evict(long userId) {
        try {
            redis.delete(PREFIX + userId);
        } catch (RuntimeException ex) {
            log.warn("evict authority cache failed for user={}", userId, ex);
        }
    }
}
