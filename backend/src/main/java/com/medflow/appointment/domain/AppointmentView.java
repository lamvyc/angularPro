package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AppointmentView(
        long id,
        String appointmentNo,
        String status,
        long patientId,
        String patientName,
        long deptId,
        String deptName,
        long doctorId,
        String doctorName,
        long scheduleId,
        long slotId,
        LocalDate appointmentDate,
        String period,
        int seqNo,
        Long encounterId,
        Long chargeId,
        String chargeStatus,
        BigDecimal chargeAmount,
        Instant createdAt) {
}
