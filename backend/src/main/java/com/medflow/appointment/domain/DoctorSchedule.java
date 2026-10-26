package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record DoctorSchedule(
        long id,
        long deptId,
        String deptName,
        long doctorId,
        String doctorName,
        LocalDate scheduleDate,
        String period,
        LocalTime startTime,
        LocalTime endTime,
        String room,
        BigDecimal registrationFee,
        String status,
        int availableCount) {
}
