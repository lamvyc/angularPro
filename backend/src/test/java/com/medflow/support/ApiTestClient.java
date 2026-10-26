package com.medflow.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 集成测试 HTTP 客户端。只依赖 JSON 文本与 JsonNode，不引入生产 DTO。
 */
public final class ApiTestClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String baseUrl;

    public ApiTestClient(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public Response get(String path, String token) {
        return send("GET", path, null, token);
    }

    public Response post(String path, Object body, String token) {
        return send("POST", path, body, token);
    }

    public Response put(String path, Object body, String token) {
        return send("PUT", path, body, token);
    }

    public Response patch(String path, Object body, String token) {
        return send("PATCH", path, body, token);
    }

    public Response login(String username, String password) {
        return post("/api/v1/auth/login", Map.of("username", username, "password", password), null);
    }

    public Response send(String method, String path, Object body, String token) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            if (body != null) {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = response.body() == null || response.body().isBlank()
                    ? null
                    : JSON.readTree(response.body());
            return new Response(response.statusCode(), json, response.body(), response.headers());
        } catch (Exception ex) {
            throw new IllegalStateException("HTTP call failed: " + method + " " + path, ex);
        }
    }

    public record Response(int status, JsonNode body, String rawBody, java.net.http.HttpHeaders headers) {

        public String text(String field) {
            return body.path(field).asString();
        }

        public boolean hasHeader(String name) {
            return headers.firstValue(name).isPresent();
        }
    }
}
