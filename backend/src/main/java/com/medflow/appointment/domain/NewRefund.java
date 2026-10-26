package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record NewRefund(String refundNo, long chargeId, String bizType, long bizId, BigDecimal amount,
        String reason, String status, long operatorId, Instant refundedAt, String idemKey) {
}
