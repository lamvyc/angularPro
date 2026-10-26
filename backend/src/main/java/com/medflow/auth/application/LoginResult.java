package com.medflow.auth.application;

import com.medflow.auth.domain.IssuedAccessToken;
import com.medflow.auth.domain.UserProfile;

public record LoginResult(IssuedAccessToken accessToken, String refreshToken, UserProfile user) {
}
