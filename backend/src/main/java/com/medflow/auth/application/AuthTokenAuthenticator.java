package com.medflow.auth.application;

import java.util.Optional;

import com.medflow.auth.domain.RefreshTokenStore;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.JwtAuthenticator;
import com.medflow.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenAuthenticator implements JwtAuthenticator {

    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final PrincipalService principalService;

    @Override
    public Optional<CurrentUser> authenticate(String rawToken) {
        try {
            Jwt jwt = jwtService.decode(rawToken);
            String jti = jwt.getId();
            if (jti != null && refreshTokenStore.isAccessTokenDenied(jti)) {
                return Optional.empty();
            }
            long userId = Long.parseLong(jwt.getSubject());
            return principalService.loadCurrentUser(userId);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
