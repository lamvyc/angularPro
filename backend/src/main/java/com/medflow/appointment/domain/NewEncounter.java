package com.medflow.appointment.domain;

public record NewEncounter(String encounterNo, long appointmentId, long patientId, long deptId,
        long doctorId, String status, Long operatorId) {
}
