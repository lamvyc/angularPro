package com.medflow.auth.domain;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt, String jti) {
}
