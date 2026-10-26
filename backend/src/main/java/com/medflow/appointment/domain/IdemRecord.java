package com.medflow.appointment.domain;

public record IdemRecord(String status, String requestHash, Integer responseStatus, String responseBody,
        String errorCode) {
}
