package com.medflow.appointment.domain;

import java.time.LocalTime;

public record NewSlot(long scheduleId, int seqNo, LocalTime startTime, LocalTime endTime, int capacity) {
}
