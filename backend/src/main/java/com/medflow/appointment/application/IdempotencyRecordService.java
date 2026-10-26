package com.medflow.appointment.application;

import java.time.Instant;
import java.util.Optional;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.IdemRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 幂等记录使用独立事务，避免与业务失败一起回滚导致无法识别重试。
 */
@Service
@RequiredArgsConstructor
public class IdempotencyRecordService {

    private final AppointmentRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(long userId, String key, String method, String path, String hash, Instant expiresAt) {
        repository.insertIdempotency(userId, key, method, path, hash, "IN_PROGRESS", expiresAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(long userId, String key, int responseStatus, String body) {
        repository.completeIdempotency(userId, key, responseStatus, body);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(long userId, String key, String errorCode) {
        repository.failIdempotency(userId, key, errorCode);
    }

    public Optional<IdemRecord> find(long userId, String key) {
        return repository.findIdempotency(userId, key);
    }
}
