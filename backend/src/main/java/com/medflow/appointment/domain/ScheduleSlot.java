package com.medflow.appointment.domain;

import java.time.LocalTime;

public record ScheduleSlot(long id, long scheduleId, int seqNo, LocalTime startTime, LocalTime endTime,
        int capacity, int bookedCount, int available, String status) {
}
