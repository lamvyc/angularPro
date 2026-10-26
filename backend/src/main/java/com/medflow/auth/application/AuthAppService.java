package com.medflow.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.medflow.auth.domain.AuthoritySnapshot;
import com.medflow.auth.domain.IssuedAccessToken;
import com.medflow.auth.domain.IssuedRefreshToken;
import com.medflow.auth.domain.RefreshTokenStore;
import com.medflow.auth.domain.UserProfile;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.JwtService;
import com.medflow.common.security.SecurityUtils;
import com.medflow.iam.application.AuditService;
import com.medflow.iam.application.MenuTreeBuilder;
import com.medflow.iam.domain.LoginState;
import com.medflow.iam.domain.RoleInfo;
import com.medflow.iam.domain.UserAccount;
import com.medflow.iam.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAppService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_SECONDS = 15 * 60;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final PrincipalService principalService;
    private final MenuTreeBuilder menuTreeBuilder;
    private final AuditService auditService;
    private final Clock clock;

    public LoginResult login(LoginCommand command) {
        String username = command.username() == null ? "" : command.username().trim();
        Instant now = Instant.now(clock);
        UserAccount user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            auditService.failure("AUTH_LOGIN_FAILED", "USER", username, ErrorCode.AUTH_INVALID.code(),
                    null, username);
            throw new BusinessException(ErrorCode.AUTH_INVALID);
        }
        if ("DISABLED".equals(user.status())) {
            auditService.failure("AUTH_LOGIN_FAILED", "USER", username, ErrorCode.AUTH_USER_DISABLED.code(),
                    user.id(), user.realName());
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
        }
        if ("LOCKED".equals(user.status())) {
            if (user.lockedUntil() != null && !user.lockedUntil().isAfter(now)) {
                userRepository.resetLoginState(user.id());
            } else {
                auditService.failure("AUTH_LOGIN_FAILED", "USER", username, ErrorCode.AUTH_LOCKED.code(),
                        user.id(), user.realName());
                throw new BusinessException(ErrorCode.AUTH_LOCKED);
            }
        }

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            LoginState state = userRepository.recordLoginFailure(user.id(), MAX_FAILED_ATTEMPTS, now.plusSeconds(LOCK_SECONDS));
            boolean locked = "LOCKED".equals(state.status());
            ErrorCode code = locked ? ErrorCode.AUTH_LOCKED : ErrorCode.AUTH_INVALID;
            auditService.failure("AUTH_LOGIN_FAILED", "USER", username, code.code(), user.id(), user.realName());
            throw new BusinessException(code);
        }

        userRepository.recordLoginSuccess(user.id(), now);
        AuthoritySnapshot snapshot = principalService.load(user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID));
        IssuedRefreshToken refreshToken = refreshTokenStore.issue(user.id(), UUID.randomUUID().toString());
        IssuedAccessToken accessToken = jwtService.issue(
                user.id(), user.username(), user.deptId(), RoleInfo.codes(snapshot.roles()));
        auditService.successFor(user.id(), user.realName(), "AUTH_LOGIN_SUCCESS", "USER",
                user.username(), null,
                Map.of("deptName", user.deptName(), "roles", RoleInfo.codes(snapshot.roles())));
        return new LoginResult(accessToken, refreshToken.value(), toProfile(snapshot));
    }

    public TokenPair refresh(String rawRefreshToken) {
        RefreshTokenStore.RotationResult rotation = refreshTokenStore.rotate(rawRefreshToken);
        if (rotation.status() != RefreshTokenStore.RotationStatus.ACTIVE || rotation.userId() == null) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        UserAccount user = userRepository.findById(rotation.userId()).orElse(null);
        if (user == null || !"ENABLED".equals(user.status())) {
            refreshTokenStore.revokeAll(rotation.userId());
            throw new BusinessException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        AuthoritySnapshot snapshot = principalService.load(user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_INVALID));
        IssuedAccessToken accessToken = jwtService.issue(
                user.id(), user.username(), user.deptId(), RoleInfo.codes(snapshot.roles()));
        return new TokenPair(accessToken, rotation.rawToken());
    }

    public void logout(String rawAccessToken, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenStore.revoke(rawRefreshToken);
        }
        if (rawAccessToken != null && !rawAccessToken.isBlank()) {
            try {
                Jwt jwt = jwtService.decode(rawAccessToken);
                refreshTokenStore.denyAccessToken(jwt.getId(), jwtService.remainingTtl(jwt));
            } catch (RuntimeException ignored) {
                // 令牌已失效时登出仍然幂等成功
            }
        }
        auditService.success("AUTH_LOGOUT", "USER", null, null, null);
    }

    public UserProfile me() {
        CurrentUser currentUser = SecurityUtils.requireCurrentUser();
        AuthoritySnapshot snapshot = principalService.load(currentUser.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID));
        return toProfile(snapshot);
    }

    private UserProfile toProfile(AuthoritySnapshot snapshot) {
        List<UserProfile.RoleView> roles = snapshot.roles().stream()
                .map(role -> new UserProfile.RoleView(role.code(), role.name(), role.dataScope()))
                .toList();
        return new UserProfile(
                snapshot.userId(),
                snapshot.username(),
                snapshot.realName(),
                snapshot.deptId(),
                snapshot.deptName(),
                snapshot.title(),
                roles,
                snapshot.permissions(),
                snapshot.dataScope(),
                menuTreeBuilder.build(snapshot.menus()));
    }
}
