package com.medflow.auth.api;

import com.medflow.auth.api.dto.LoginRequest;
import com.medflow.auth.api.dto.LoginResponse;
import com.medflow.auth.api.dto.LogoutRequest;
import com.medflow.auth.api.dto.RefreshRequest;
import com.medflow.auth.api.dto.TokenResponse;
import com.medflow.auth.application.AuthAppService;
import com.medflow.auth.application.LoginCommand;
import com.medflow.auth.application.LoginResult;
import com.medflow.auth.application.TokenPair;
import com.medflow.auth.domain.UserProfile;
import com.medflow.common.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "登录、刷新令牌、登出与当前用户")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authAppService.login(new LoginCommand(request.username(), request.password()));
        return new LoginResponse(result.accessToken().value(), result.refreshToken(),
                jwtService.accessTokenSeconds(), "Bearer", result.user());
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌（刷新令牌一次性轮换）")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair pair = authAppService.refresh(request.refreshToken());
        return new TokenResponse(pair.accessToken().value(), pair.refreshToken(),
                jwtService.accessTokenSeconds(), "Bearer");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "登出（吊销访问令牌与刷新令牌）")
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        authAppService.logout(bearerToken(authorization), request == null ? null : request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户、权限与菜单")
    public UserProfile me() {
        return authAppService.me();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
