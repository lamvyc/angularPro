package com.medflow.appointment.api.dto;

import jakarta.validation.constraints.NotNull;

public record BookAppointmentRequest(
        @NotNull(message = "患者不能为空") Long patientId,
        @NotNull(message = "号源不能为空") Long slotId) {
}
