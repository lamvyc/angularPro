package com.medflow.appointment.domain;

import java.time.LocalDate;

public record Patient(
        long id,
        String patientNo,
        String name,
        String gender,
        LocalDate birthDate,
        String idCardMasked,
        String phone,
        String address,
        String allergyHistory,
        String status) {
}
