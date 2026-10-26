package com.medflow.encounter.domain;

import java.util.Map;
import java.util.Set;

public enum EncounterStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<EncounterStatus, Set<EncounterStatus>> TRANSITIONS = Map.of(
            WAITING, Set.of(IN_PROGRESS, CANCELLED),
            IN_PROGRESS, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of());

    public boolean canTransitionTo(EncounterStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
