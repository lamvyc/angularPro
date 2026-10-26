package com.medflow.appointment.api.dto;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(@Size(max = 255) String reason) {
}
