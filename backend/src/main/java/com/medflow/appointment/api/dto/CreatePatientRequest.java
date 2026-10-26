package com.medflow.appointment.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePatientRequest(
        @NotBlank(message = "患者姓名不能为空")
        @Size(max = 50)
        String name,

        String gender,

        @NotNull(message = "出生日期不能为空")
        LocalDate birthDate,

        @Size(max = 32)
        String idCardNo,

        @Size(max = 20)
        String phone,

        @Size(max = 255)
        String address,

        @Size(max = 500)
        String allergyHistory) {
}
