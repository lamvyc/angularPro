package com.medflow.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.medflow.auth.domain.IssuedAccessToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
    }

    public IssuedAccessToken issue(long userId, String username, Long deptId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("username", username)
                .claim("deptId", deptId)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt, jti);
    }

    public Jwt decode(String rawToken) {
        return decoder.decode(rawToken);
    }

    public long accessTokenSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    public Duration remainingTtl(Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
