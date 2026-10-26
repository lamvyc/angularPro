package com.medflow.appointment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import com.medflow.appointment.domain.IdemRecord;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordService recordService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public <T> IdempotentResult<T> execute(long userId, String key, String method, String path,
            Object request, Class<T> responseType, int successStatus, Supplier<T> action) {
        if (key == null || key.isBlank() || key.length() > 80) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "缺少或非法的 Idempotency-Key");
        }
        String requestHash = hash(jsonMapper.writeValueAsString(request));
        try {
            recordService.start(userId, key, method, path, requestHash,
                    Instant.now(clock).plus(Duration.ofHours(24)));
        } catch (DuplicateKeyException duplicate) {
            IdemRecord existing = recordService.find(userId, key)
                    .orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENT_IN_PROGRESS));
            if (!requestHash.equals(existing.requestHash())) {
                throw new BusinessException(ErrorCode.CONFLICT, "同一幂等键不能用于不同请求");
            }
            if ("COMPLETED".equals(existing.status())) {
                T value = jsonMapper.readValue(existing.responseBody(), responseType);
                return new IdempotentResult<>(value, true, existing.responseStatus());
            }
            if ("IN_PROGRESS".equals(existing.status())) {
                throw new BusinessException(ErrorCode.IDEMPOTENT_IN_PROGRESS);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "上次请求失败，请使用新的幂等键重试");
        }

        try {
            T value = action.get();
            recordService.complete(userId, key, successStatus, jsonMapper.writeValueAsString(value));
            return new IdempotentResult<>(value, false, successStatus);
        } catch (BusinessException ex) {
            recordService.fail(userId, key, ex.errorCode().code());
            throw ex;
        } catch (RuntimeException ex) {
            recordService.fail(userId, key, ErrorCode.INTERNAL.code());
            throw ex;
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
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

    public record IdempotentResult<T>(T value, boolean replay, int status) {
    }
}
