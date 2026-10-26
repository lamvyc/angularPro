package com.medflow.auth;

import java.util.ArrayList;
import java.util.List;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        resetLoginState();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @AfterEach
    void tearDown() {
        resetLoginState();
    }

    @Test
    void should_login_and_return_profile_with_permissions_and_menus() {
        var response = api.login("admin", "Medflow@2026");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.text("accessToken")).isNotBlank();
        assertThat(response.text("refreshToken")).isNotBlank();
        assertThat(response.body().path("expiresIn").asInt()).isEqualTo(900);

        var user = response.body().path("user");
        assertThat(user.path("username").asString()).isEqualTo("admin");
        assertThat(user.path("deptName").asString()).isEqualTo("行政管理");
        assertThat(user.path("dataScope").asString()).isEqualTo("ALL");
        assertThat(stringValues(user.path("permissions"))).contains("system:user:read");
        assertThat(menuCodes(user.path("menus"))).contains("menu:system", "menu:dashboard");
    }

    @Test
    void should_return_different_menus_for_doctor() {
        var response = api.login("doctor1", "Medflow@2026");

        assertThat(response.status()).isEqualTo(200);
        var user = response.body().path("user");
        assertThat(user.path("deptName").asString()).isEqualTo("皮肤科");
        assertThat(user.path("dataScope").asString()).isEqualTo("DEPT");
        assertThat(stringValues(user.path("permissions")))
                .contains("schedule:read", "encounter:read")
                .doesNotContain("system:user:read");
        assertThat(menuCodes(user.path("menus")))
                .contains("menu:dashboard", "menu:staff")
                .doesNotContain("menu:system");
    }

    @Test
    void should_reject_wrong_password() {
        var response = api.login("admin", "wrong-password");

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.body().path("code").asString()).isEqualTo("MED-AUTH-001");
        assertThat(response.body().path("requestId").asString()).isNotBlank();
    }

    @Test
    void should_reject_disabled_user() {
        var response = api.login("nurse2", "Medflow@2026");

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.body().path("code").asString()).isEqualTo("MED-AUTH-004");
    }

    @Test
    void should_lock_account_after_five_failures_and_recover_after_unlock() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThat(api.login("doctor2", "wrong").status()).isEqualTo(401);
        }

        var locked = api.login("doctor2", "wrong");
        assertThat(locked.status()).isEqualTo(423);
        assertThat(locked.body().path("code").asString()).isEqualTo("MED-AUTH-002");

        var correctWhileLocked = api.login("doctor2", "Medflow@2026");
        assertThat(correctWhileLocked.status()).isEqualTo(423);

        jdbcTemplate.update("""
                UPDATE sys_user SET status = 'ENABLED', failed_attempts = 0, locked_until = NULL
                WHERE username = 'doctor2'
                """);

        assertThat(api.login("doctor2", "Medflow@2026").status()).isEqualTo(200);
    }

    @Test
    void should_rotate_refresh_token_and_reject_replay() {
        var login = api.login("doctor1", "Medflow@2026");
        String oldRefresh = login.text("refreshToken");

        var rotated = api.post("/api/v1/auth/refresh", java.util.Map.of("refreshToken", oldRefresh), null);
        assertThat(rotated.status()).isEqualTo(200);
        String newRefresh = rotated.text("refreshToken");
        assertThat(newRefresh).isNotBlank().isNotEqualTo(oldRefresh);
        assertThat(rotated.text("accessToken")).isNotBlank();

        var replay = api.post("/api/v1/auth/refresh", java.util.Map.of("refreshToken", oldRefresh), null);
        assertThat(replay.status()).isEqualTo(401);

        var afterReplay = api.post("/api/v1/auth/refresh", java.util.Map.of("refreshToken", newRefresh), null);
        assertThat(afterReplay.status()).isEqualTo(401);
    }

    @Test
    void should_revoke_access_and_refresh_token_on_logout() {
        var login = api.login("cashier1", "Medflow@2026");
        String access = login.text("accessToken");
        String refresh = login.text("refreshToken");

        assertThat(api.get("/api/v1/auth/me", access).status()).isEqualTo(200);

        var logout = api.post("/api/v1/auth/logout", java.util.Map.of("refreshToken", refresh), access);
        assertThat(logout.status()).isEqualTo(204);

        assertThat(api.get("/api/v1/auth/me", access).status()).isEqualTo(401);
        assertThat(api.post("/api/v1/auth/refresh", java.util.Map.of("refreshToken", refresh), null).status())
                .isEqualTo(401);
    }

    @Test
    void should_reject_missing_token() {
        assertThat(api.get("/api/v1/auth/me", null).status()).isEqualTo(401);
    }

    private void resetLoginState() {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET status = 'ENABLED', failed_attempts = 0, locked_until = NULL
                WHERE username IN ('admin', 'doctor1', 'doctor2', 'cashier1')
                """);
    }

    private List<String> stringValues(tools.jackson.databind.JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asString()));
        return values;
    }

    private List<String> menuCodes(tools.jackson.databind.JsonNode menus) {
        List<String> codes = new ArrayList<>();
        menus.forEach(menu -> {
            codes.add(menu.path("code").asString());
            codes.addAll(menuCodes(menu.path("children")));
        });
        return codes;
    }
}
