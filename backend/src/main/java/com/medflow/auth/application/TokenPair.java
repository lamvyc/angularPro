package com.medflow.auth.application;

import com.medflow.auth.domain.IssuedAccessToken;

public record TokenPair(IssuedAccessToken accessToken, String refreshToken) {
}
