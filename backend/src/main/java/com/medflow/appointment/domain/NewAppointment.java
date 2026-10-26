package com.medflow.appointment.domain;

import java.time.LocalDate;

public record NewAppointment(String appointmentNo, long patientId, long deptId, long doctorId,
        long scheduleId, long slotId, LocalDate appointmentDate, String period, int seqNo,
        String status, String source, Long bookedBy) {
}
