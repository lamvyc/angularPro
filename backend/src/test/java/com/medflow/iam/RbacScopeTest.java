package com.medflow.iam;

import java.util.ArrayList;
import java.util.List;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

class RbacScopeTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private ApiTestClient api;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
    }

    @Test
    void doctor_should_only_see_doctors_in_own_department() {
        String token = tokenFor("doctor1");

        var response = api.get("/api/v1/staff/doctors", token);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body().isArray()).isTrue();
        List<Long> deptIds = new ArrayList<>();
        response.body().forEach(node -> deptIds.add(node.path("deptId").asLong()));
        assertThat(deptIds).isNotEmpty().allMatch(id -> id == 1L);
        assertThat(response.rawBody()).doesNotContain("李静");
    }

    @Test
    void doctor_should_be_forbidden_to_query_another_department() {
        String token = tokenFor("doctor1");

        var response = api.get("/api/v1/staff/doctors?deptId=2", token);

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.body().path("code").asString()).isEqualTo("MED-AUTHZ-001");
    }

    @Test
    void cashier_should_only_see_own_department_doctors() {
        String token = tokenFor("cashier1");

        var response = api.get("/api/v1/staff/doctors", token);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.rawBody()).contains("李静").doesNotContain("张明");
    }

    @Test
    void roles_without_schedule_permission_should_be_forbidden() {
        assertThat(api.get("/api/v1/staff/doctors", tokenFor("admin")).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/staff/doctors", tokenFor("pharmacist1")).status()).isEqualTo(403);
    }

    @Test
    void admin_should_read_permission_tree_but_doctor_should_not() {
        var adminResponse = api.get("/api/v1/permissions/tree", tokenFor("admin"));
        assertThat(adminResponse.status()).isEqualTo(200);
        assertThat(adminResponse.body().isArray()).isTrue();
        assertThat(flattenCodes(adminResponse.body()))
                .contains("menu:dashboard", "system:user:read", "system:user:create", "system:audit:read");

        var doctorResponse = api.get("/api/v1/permissions/tree", tokenFor("doctor1"));
        assertThat(doctorResponse.status()).isEqualTo(403);
        assertThat(doctorResponse.body().path("code").asString()).isEqualTo("MED-AUTHZ-001");
    }

    @Test
    void staff_endpoint_should_require_authentication() {
        assertThat(api.get("/api/v1/staff/doctors", null).status()).isEqualTo(401);
    }

    private String tokenFor(String username) {
        var response = api.login(username, "Medflow@2026");
        assertThat(response.status()).as("login %s", username).isEqualTo(200);
        return response.text("accessToken");
    }

    private List<String> flattenCodes(JsonNode nodes) {
        List<String> codes = new ArrayList<>();
        nodes.forEach(node -> {
            codes.add(node.path("code").asString());
            codes.addAll(flattenCodes(node.path("children")));
        });
        return codes;
    }
}
