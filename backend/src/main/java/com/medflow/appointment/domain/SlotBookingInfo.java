package com.medflow.appointment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record SlotBookingInfo(
        long slotId,
        long scheduleId,
        String slotStatus,
        int capacity,
        int bookedCount,
        String scheduleStatus,
        LocalDate scheduleDate,
        String period,
        long doctorId,
        long deptId,
        BigDecimal registrationFee,
        LocalTime startTime) {
}
