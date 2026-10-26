package com.medflow.encounter.domain;

import java.time.Instant;
import java.time.LocalDate;

public record EncounterRow(
        long id,
        String encounterNo,
        String status,
        long patientId,
        String patientName,
        String patientNo,
        long deptId,
        String deptName,
        long doctorId,
        String doctorName,
        long appointmentId,
        LocalDate appointmentDate,
        String period,
        int seqNo,
        String chiefComplaint,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt) {
}
