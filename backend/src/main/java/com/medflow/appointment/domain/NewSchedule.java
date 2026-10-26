package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record NewSchedule(long deptId, long doctorId, LocalDate scheduleDate, String period,
        LocalTime startTime, LocalTime endTime, String room, BigDecimal registrationFee,
        Instant publishedAt, Long publishedBy) {
}
