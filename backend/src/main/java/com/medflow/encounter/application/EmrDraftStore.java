package com.medflow.encounter.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 草稿副本：数据库仍是唯一事实源，Redis 仅用于快速恢复提示。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmrDraftStore {

    private static final String PREFIX = "emr:draft:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public void put(long recordId, Map<String, Object> content, int version, Instant updatedAt) {
        try {
            redis.opsForValue().set(PREFIX + recordId,
                    jsonMapper.writeValueAsString(new Snapshot(content, version, updatedAt)), TTL);
        } catch (RuntimeException ex) {
            log.warn("write emr draft cache failed record={}", recordId, ex);
        }
    }

    public Optional<Snapshot> get(long recordId) {
        try {
            String value = redis.opsForValue().get(PREFIX + recordId);
            return value == null ? Optional.empty() : Optional.of(jsonMapper.readValue(value, Snapshot.class));
        } catch (RuntimeException ex) {
            log.warn("read emr draft cache failed record={}", recordId, ex);
            return Optional.empty();
        }
    }

    public void delete(long recordId) {
        try {
            redis.delete(PREFIX + recordId);
        } catch (RuntimeException ex) {
            log.warn("delete emr draft cache failed record={}", recordId, ex);
        }
    }

    public record Snapshot(Map<String, Object> content, int version, Instant updatedAt) {
    }
}
