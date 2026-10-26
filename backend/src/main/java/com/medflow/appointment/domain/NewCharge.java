package com.medflow.appointment.domain;

import java.math.BigDecimal;

public record NewCharge(String orderNo, String bizType, long bizId, long patientId, long deptId,
        BigDecimal amount, String status, Long operatorId) {
}
