package com.medflow.iam.domain;

import java.time.Instant;

public record LoginState(int failedAttempts, String status, Instant lockedUntil) {
}
