package com.medflow.appointment.domain;

import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {
    BOOKED,
    CHECKED_IN,
    IN_CONSULTATION,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            BOOKED, Set.of(CHECKED_IN, CANCELLED, NO_SHOW),
            CHECKED_IN, Set.of(IN_CONSULTATION, CANCELLED, NO_SHOW),
            IN_CONSULTATION, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of(),
            NO_SHOW, Set.of());

    public boolean canTransitionTo(AppointmentStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static AppointmentStatus of(String value) {
        try {
            return valueOf(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("未知预约状态: " + value, ex);
        }
    }
}
