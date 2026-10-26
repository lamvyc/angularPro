package com.medflow.appointment.domain;

import java.time.LocalDate;

public record NewPatient(String patientNo, String name, String gender, LocalDate birthDate,
        String idCardHash, String idCardMasked, String phone, String address, String allergyHistory,
        Long operatorId) {
}
