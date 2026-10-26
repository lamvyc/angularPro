package com.medflow.appointment.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleCommand(LocalDate scheduleDate, String period, LocalTime startTime,
        LocalTime endTime, String room, int slotCount, int capacityPerSlot, BigDecimal registrationFee) {
}
