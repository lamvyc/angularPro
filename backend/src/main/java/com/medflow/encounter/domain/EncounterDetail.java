package com.medflow.encounter.domain;

import java.util.List;

public record EncounterDetail(
        EncounterRow encounter,
        EmrView emr,
        List<DiagnosisView> diagnoses,
        EmrDraftView draft) {

    public EncounterDetail {
        diagnoses = diagnoses == null ? List.of() : List.copyOf(diagnoses);
    }
}
