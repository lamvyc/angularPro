package com.medflow.appointment.application;

import java.time.LocalDate;

public record CreatePatientCommand(String name, String gender, LocalDate birthDate, String idCardNo,
        String phone, String address, String allergyHistory) {
}
