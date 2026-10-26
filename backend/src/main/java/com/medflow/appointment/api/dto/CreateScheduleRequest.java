package com.medflow.appointment.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateScheduleRequest(
        @NotNull(message = "排班日期不能为空")
        LocalDate scheduleDate,

        @Pattern(regexp = "AM|PM", message = "时段只能是 AM 或 PM")
        String period,

        @NotNull(message = "开始时间不能为空")
        LocalTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalTime endTime,

        String room,

        @Min(value = 1, message = "号源数量至少 1")
        @Max(value = 50, message = "号源数量最多 50")
        int slotCount,

        @Min(value = 1, message = "容量至少 1")
        @Max(value = 20, message = "容量最多 20")
        int capacityPerSlot,

        @DecimalMin(value = "0.00", message = "挂号费不能为负")
        BigDecimal registrationFee) {
}
