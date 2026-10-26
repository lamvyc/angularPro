package com.medflow.appointment.api.dto;

import jakarta.validation.constraints.Pattern;

public record PayChargeRequest(
        @Pattern(regexp = "CASH|WECHAT|ALIPAY|CARD", message = "不支持的支付方式")
        String paymentMethod) {
}
