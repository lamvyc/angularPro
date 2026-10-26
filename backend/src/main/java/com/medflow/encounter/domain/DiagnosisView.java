package com.medflow.encounter.domain;

public record DiagnosisView(long id, String diagnosisCode, String diagnosisName, String type, String note,
        int sortNo) {
}
