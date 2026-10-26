package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ChargeView(long id, String orderNo, String bizType, long bizId, long patientId, long deptId,
        BigDecimal amount, String status, String paymentMethod, Instant paidAt, Long cashierId) {
}
