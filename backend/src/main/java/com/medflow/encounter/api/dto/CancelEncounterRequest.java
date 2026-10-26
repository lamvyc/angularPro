package com.medflow.encounter.api.dto;

import jakarta.validation.constraints.Size;

public record CancelEncounterRequest(@Size(max = 255) String reason) {
}
