package com.medflow.encounter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

class EmrFlowTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private ApiTestClient api;
    private String doctorToken;
    private String cashierToken;
    private String nurseToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        doctorToken = token("doctor2");
        cashierToken = token("cashier1");
        nurseToken = token("nurse3");
        adminToken = token("admin");
    }

    @Test
    void should_validate_template_sign_and_revise_with_history() {
        long encounterId = createEncounter("结构病历患者");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);

        var invalid = api.post("/api/v1/encounters/" + encounterId + "/emr/sign",
                Map.of("content", Map.of("chiefComplaint", "发热", "fever", true), "version", 0),
                doctorToken);
        assertThat(invalid.status()).isEqualTo(422);
        assertThat(invalid.rawBody()).contains("temperature");

        var signed = api.post("/api/v1/encounters/" + encounterId + "/emr/sign",
                Map.of("content", Map.of("chiefComplaint", "发热", "fever", true, "temperature", 38.5,
                        "presentIllness", "受凉后发热"), "version", 0), doctorToken);
        assertThat(signed.status()).isEqualTo(200);
        assertThat(signed.text("status")).isEqualTo("SIGNED");
        assertThat(signed.body().path("signatureHash").asString()).hasSize(64);
        assertThat(signed.body().path("templateName").asString()).isNotBlank();

        var editAfterSign = api.put("/api/v1/encounters/" + encounterId + "/emr",
                Map.of("content", Map.of("chiefComplaint", "覆盖"), "version", 1), doctorToken);
        assertThat(editAfterSign.status()).isEqualTo(409);

        var finishWithoutDiagnosis = api.post("/api/v1/encounters/" + encounterId + "/finish", null, doctorToken);
        assertThat(finishWithoutDiagnosis.status()).isEqualTo(409);

        api.put("/api/v1/encounters/" + encounterId + "/diagnoses",
                Map.of("diagnoses", List.of(Map.of(
                        "code", "J06.9", "name", "急性上呼吸道感染", "type", "PRIMARY"))), doctorToken);
        assertThat(api.post("/api/v1/encounters/" + encounterId + "/finish", null, doctorToken).status())
                .isEqualTo(200);

        var amended = api.post("/api/v1/encounters/" + encounterId + "/emr/amend",
                Map.of("content", Map.of("chiefComplaint", "发热（修订）", "fever", true, "temperature", 38.5,
                        "presentIllness", "受凉后发热，补充流行病学史"), "version", 1,
                        "reason", "补充流行病学史"), doctorToken);
        assertThat(amended.status()).isEqualTo(200);
        assertThat(amended.text("status")).isEqualTo("AMENDED");
        assertThat(amended.body().path("version").asInt()).isEqualTo(2);

        var versions = api.get("/api/v1/encounters/" + encounterId + "/emr/versions", doctorToken);
        assertThat(versions.status()).isEqualTo(200);
        assertThat(versions.rawBody()).contains("SUPERSEDED").contains("SIGNED");

        var version1 = api.get("/api/v1/encounters/" + encounterId + "/emr/versions/1", doctorToken);
        assertThat(version1.status()).isEqualTo(200);
        assertThat(version1.body().path("content").path("chiefComplaint").asString()).isEqualTo("发热");
        assertThat(version1.body().path("changeReason").asString()).isEmpty();
    }

    @Test
    void should_manage_template_versions_and_archive() {
        var created = api.post("/api/v1/emr-templates", Map.of(
                "code", "TEST_" + System.nanoTime(),
                "name", "测试模板",
                "scopeType", "HOSPITAL",
                "schema", Map.of("sections", List.of(Map.of(
                        "key", "basic", "title", "基本信息",
                        "fields", List.of(Map.of(
                                "key", "note", "type", "textarea", "label", "说明", "required", true))))),
                "changelog", "初始版本"), adminToken);
        assertThat(created.status()).isEqualTo(200);
        long templateId = created.body().path("id").asLong();
        assertThat(created.body().path("versionNo").asInt()).isEqualTo(1);

        var published = api.put("/api/v1/emr-templates/" + templateId, Map.of(
                "schema", Map.of("sections", List.of(Map.of(
                        "key", "basic", "title", "基本信息 V2",
                        "fields", List.of(Map.of(
                                "key", "note", "type", "textarea", "label", "说明", "required", true))))),
                "changelog", "第二版"), adminToken);
        assertThat(published.status()).isEqualTo(200);
        assertThat(published.body().path("versionNo").asInt()).isEqualTo(2);

        var versions = api.get("/api/v1/emr-templates/" + templateId + "/versions", doctorToken);
        assertThat(versions.status()).isEqualTo(200);
        assertThat(versions.body().size()).isEqualTo(2);

        assertThat(api.post("/api/v1/emr-templates", Map.of(
                "code", "FORBIDDEN", "name", "越权", "scopeType", "HOSPITAL",
                "schema", Map.of("sections", List.of())), doctorToken).status()).isEqualTo(403);

        var archived = api.post("/api/v1/emr-templates/" + templateId + "/archive", null, adminToken);
        assertThat(archived.status()).isEqualTo(200);
        assertThat(archived.text("status")).isEqualTo("ARCHIVED");
    }

    private long createEncounter(String patientName) {
        LocalDate today = LocalDate.now();
        var existing = api.get("/api/v1/schedules?date=" + today + "&doctorId=3", cashierToken);
        long scheduleId = -1;
        for (var schedule : existing.body()) {
            if ("AM".equals(schedule.path("period").asString())) {
                scheduleId = schedule.path("id").asLong();
                break;
            }
        }
        if (scheduleId < 0) {
            var created = api.post("/api/v1/schedules", Map.of(
                    "scheduleDate", today.toString(), "period", "AM",
                    "startTime", "08:00", "endTime", "12:00", "room", "结构病历诊室",
                    "slotCount", 20, "capacityPerSlot", 1, "registrationFee", 20), doctorToken);
            assertThat(created.status()).isEqualTo(201);
            scheduleId = created.body().path("id").asLong();
        }
        var slots = api.get("/api/v1/schedules/" + scheduleId + "/slots", cashierToken);
        long slotId = -1;
        for (var slot : slots.body()) {
            if (slot.path("available").asInt() > 0) {
                slotId = slot.path("id").asLong();
                break;
            }
        }
        assertThat(slotId).isPositive();
        var patient = api.post("/api/v1/patients", Map.of(
                "name", patientName, "gender", "MALE", "birthDate", "1990-01-01",
                "idCardNo", "31010119900101" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "phone", "1396666" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "address", "上海", "allergyHistory", "无"), cashierToken);
        assertThat(patient.status()).isEqualTo(201);
        long patientId = patient.body().path("id").asLong();
        var booked = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", "emr-" + System.nanoTime()));
        assertThat(booked.status()).isEqualTo(201);
        long appointmentId = booked.body().path("id").asLong();
        var checkedIn = api.post("/api/v1/appointments/" + appointmentId + "/check-in", null, nurseToken);
        assertThat(checkedIn.status()).isEqualTo(200);
        return checkedIn.body().path("encounterId").asLong();
    }

    private String token(String username) {
        var response = api.login(username, "Medflow@2026");
        assertThat(response.status()).as("login " + username).isEqualTo(200);
        return response.text("accessToken");
    }
}
