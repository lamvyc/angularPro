package com.medflow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端骨架测试：真实 MySQL + Redis，验证 Flyway、安全基线、Problem Details 与公开探针。
 */
class ApplicationSmokeTest extends AbstractIntegrationTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${local.server.port}")
    private int port;

    @Test
    void should_start_with_migrations_and_healthy_dependencies() throws Exception {
        HttpResponse<String> health = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            health = get("/actuator/health");
            if (health.statusCode() == 200) {
                break;
            }
            Thread.sleep(500);
        }

        assertThat(health).isNotNull();
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void should_expose_public_ping() throws Exception {
        HttpResponse<String> response = get("/api/v1/system/ping");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("pong");
        assertThat(response.headers().firstValue("X-Request-Id")).isPresent();
    }

    @Test
    void should_reject_unauthenticated_business_request_with_problem_details() throws Exception {
        HttpResponse<String> response = get("/api/v1/system/info");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("application/problem+json");
        assertThat(response.body())
                .contains("\"code\":\"MED-AUTH-001\"")
                .contains("\"requestId\"");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
