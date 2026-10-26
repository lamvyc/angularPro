package com.medflow.iam;

import java.util.List;
import java.util.Map;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class IamAdminTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApiTestClient api;
    private String adminToken;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        cleanup();
        adminToken = tokenFor("admin", "Medflow@2026");
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void should_create_department_and_reject_duplicate_code() {
        var created = api.post("/api/v1/departments",
                Map.of("code", "M1DEPT", "name", "M1 测试科", "type", "CLINICAL", "sortNo", 99), adminToken);
        assertThat(created.status()).isEqualTo(201);
        long deptId = created.body().path("id").asLong();

        var duplicate = api.post("/api/v1/departments",
                Map.of("code", "M1DEPT", "name", "重复科室", "type", "CLINICAL", "sortNo", 100), adminToken);
        assertThat(duplicate.status()).isEqualTo(409);

        var list = api.get("/api/v1/departments", adminToken);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.rawBody()).contains("M1DEPT").contains(String.valueOf(deptId));
    }

    @Test
    void should_create_role_user_and_invalidate_permission_cache() {
        long deptId = createDepartment("M1DEPT");
        long roleId = createRole("M1TEST", "M1 测试角色", "ALL");

        var assign = api.put("/api/v1/roles/%d/permissions".formatted(roleId),
                Map.of("permissionIds", List.of(1, 2, 7)), adminToken);
        assertThat(assign.status()).isEqualTo(204);

        var created = api.post("/api/v1/users", Map.of(
                "username", "m1user",
                "password", "Medflow@2026",
                "realName", "测试用户",
                "deptId", deptId,
                "title", "主治医师",
                "phone", "13900000001",
                "roleIds", List.of(roleId)), adminToken);
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.body().path("username").asString()).isEqualTo("m1user");

        var login = api.login("m1user", "Medflow@2026");
        assertThat(login.status()).isEqualTo(200);
        String token = login.text("accessToken");
        assertThat(login.body().path("user").path("permissions").toString())
                .contains("menu:dashboard", "system:audit:read");

        assertThat(api.get("/api/v1/audit-logs?size=5", token).status()).isEqualTo(200);

        // 授权变更后，已签发的访问令牌下一次请求立即失去权限（Redis 缓存被清除）
        assertThat(api.put("/api/v1/roles/%d/permissions".formatted(roleId),
                Map.of("permissionIds", List.of(1)), adminToken).status()).isEqualTo(204);

        var forbidden = api.get("/api/v1/audit-logs?size=5", token);
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.body().path("code").asString()).isEqualTo("MED-AUTHZ-001");

        var me = api.get("/api/v1/auth/me", token);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().path("permissions").toString()).doesNotContain("system:audit:read");
    }

    @Test
    void should_reject_duplicate_username_and_short_password() {
        long deptId = createDepartment("M1DEPT");
        long roleId = createRole("M1TEST", "M1 测试角色", "ALL");

        var shortPassword = api.post("/api/v1/users", Map.of(
                "username", "m1short", "password", "123",
                "realName", "短密码", "deptId", deptId, "roleIds", List.of(roleId)), adminToken);
        assertThat(shortPassword.status()).isEqualTo(422);
        assertThat(shortPassword.body().path("fieldErrors").isArray()).isTrue();

        var created = api.post("/api/v1/users", Map.of(
                "username", "m1dup", "password", "Medflow@2026",
                "realName", "重复用户", "deptId", deptId, "roleIds", List.of(roleId)), adminToken);
        assertThat(created.status()).isEqualTo(201);

        var duplicate = api.post("/api/v1/users", Map.of(
                "username", "m1dup", "password", "Medflow@2026",
                "realName", "重复用户", "deptId", deptId, "roleIds", List.of(roleId)), adminToken);
        assertThat(duplicate.status()).isEqualTo(409);
    }

    @Test
    void should_disable_user_and_reset_password() {
        long deptId = createDepartment("M1DEPT");
        long roleId = createRole("M1TEST", "M1 测试角色", "ALL");
        long userId = createUser("m1status", deptId, roleId);
        String existingToken = tokenFor("m1status", "Medflow@2026");

        assertThat(api.patch("/api/v1/users/%d/status".formatted(userId),
                Map.of("status", "DISABLED"), adminToken).status()).isEqualTo(204);
        assertThat(api.get("/api/v1/auth/me", existingToken).status()).isEqualTo(401);
        assertThat(api.login("m1status", "Medflow@2026").status()).isEqualTo(403);

        assertThat(api.patch("/api/v1/users/%d/status".formatted(userId),
                Map.of("status", "ENABLED"), adminToken).status()).isEqualTo(204);
        assertThat(api.login("m1status", "Medflow@2026").status()).isEqualTo(200);

        assertThat(api.post("/api/v1/users/%d/reset-password".formatted(userId),
                Map.of("newPassword", "NewPass@2026"), adminToken).status()).isEqualTo(204);
        assertThat(api.login("m1status", "Medflow@2026").status()).isEqualTo(401);
        assertThat(api.login("m1status", "NewPass@2026").status()).isEqualTo(200);
    }

    @Test
    void should_prevent_disabling_current_account_and_protect_builtin_admin_role() {
        var self = api.patch("/api/v1/users/1/status", Map.of("status", "DISABLED"), adminToken);
        assertThat(self.status()).isEqualTo(409);
        assertThat(self.body().path("code").asString()).isEqualTo("MED-IAM-001");

        var adminRole = api.put("/api/v1/roles/1/permissions",
                Map.of("permissionIds", List.of(1, 3, 4)), adminToken);
        assertThat(adminRole.status()).isEqualTo(409);
        assertThat(adminRole.body().path("code").asString()).isEqualTo("MED-IAM-001");

        var adminScope = api.put("/api/v1/roles/1",
                Map.of("name", "系统管理员", "dataScope", "SELF"), adminToken);
        assertThat(adminScope.status()).isEqualTo(409);
        assertThat(adminScope.body().path("code").asString()).isEqualTo("MED-IAM-001");
    }

    @Test
    void doctor_should_not_call_admin_apis() {
        String doctorToken = tokenFor("doctor1", "Medflow@2026");

        assertThat(api.get("/api/v1/users", doctorToken).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/roles", doctorToken).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/audit-logs", doctorToken).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/departments", doctorToken).status()).isEqualTo(200);

        var audit = api.get("/api/v1/audit-logs?action=ACCESS_DENIED&actorId=2&size=20", adminToken);
        assertThat(audit.status()).isEqualTo(200);
        assertThat(audit.rawBody()).contains("ACCESS_DENIED", "张明", "/api/v1/users");
    }

    @Test
    void audit_log_should_record_login_and_user_changes_without_secrets() {
        long deptId = createDepartment("M1DEPT");
        long roleId = createRole("M1TEST", "M1 测试角色", "ALL");
        createUser("m1audit", deptId, roleId);

        var response = api.get("/api/v1/audit-logs?size=100", adminToken);
        assertThat(response.status()).isEqualTo(200);
        String body = response.rawBody();
        assertThat(body).contains("AUTH_LOGIN_SUCCESS", "USER_CREATED", "DEPARTMENT_CREATED", "ROLE_CREATED")
                .contains("系统管理员");
        assertThat(body).doesNotContain("Medflow@2026").doesNotContain("accessToken")
                .doesNotContain("refreshToken");

        var filtered = api.get("/api/v1/audit-logs?action=USER_CREATED&size=10", adminToken);
        assertThat(filtered.status()).isEqualTo(200);
        assertThat(filtered.rawBody()).contains("USER_CREATED").doesNotContain("AUTH_LOGIN_SUCCESS");

        var loginAudit = api.get("/api/v1/audit-logs?action=AUTH_LOGIN_SUCCESS&size=10", adminToken);
        assertThat(loginAudit.status()).isEqualTo(200);
        assertThat(loginAudit.rawBody()).contains("\"actorName\":\"系统管理员\"");
    }

    private String tokenFor(String username, String password) {
        var response = api.login(username, password);
        assertThat(response.status()).as("login %s", username).isEqualTo(200);
        return response.text("accessToken");
    }

    private long createDepartment(String code) {
        var response = api.post("/api/v1/departments",
                Map.of("code", code, "name", "M1 测试科", "type", "CLINICAL", "sortNo", 99), adminToken);
        assertThat(response.status()).isEqualTo(201);
        return response.body().path("id").asLong();
    }

    private long createRole(String code, String name, String dataScope) {
        var response = api.post("/api/v1/roles",
                Map.of("code", code, "name", name, "dataScope", dataScope), adminToken);
        assertThat(response.status()).isEqualTo(201);
        return response.body().path("id").asLong();
    }

    private long createUser(String username, long deptId, long roleId) {
        var response = api.post("/api/v1/users", Map.of(
                "username", username,
                "password", "Medflow@2026",
                "realName", "测试用户",
                "deptId", deptId,
                "title", "主治医师",
                "phone", "13900000002",
                "roleIds", List.of(roleId)), adminToken);
        assertThat(response.status()).isEqualTo(201);
        return response.body().path("id").asLong();
    }

    private void cleanup() {
        jdbcTemplate.update("""
                DELETE FROM sys_user_role
                WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'm1%')
                """);
        jdbcTemplate.update("DELETE FROM sys_user WHERE username LIKE 'm1%'");
        jdbcTemplate.update("""
                DELETE FROM sys_role_permission
                WHERE role_id IN (SELECT id FROM sys_role WHERE code LIKE 'M1%')
                """);
        jdbcTemplate.update("DELETE FROM sys_role WHERE code LIKE 'M1%'");
        jdbcTemplate.update("DELETE FROM sys_department WHERE code LIKE 'M1%'");
    }
}
