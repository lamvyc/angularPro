package com.medflow.encounter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkstationFlowTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApiTestClient api;
    private String doctorToken;
    private String cashierToken;
    private String nurseToken;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        doctorToken = token("doctor2");
        cashierToken = token("cashier1");
        nurseToken = token("nurse3");
    }

    @Test
    void should_start_save_diagnose_and_finish_encounter() {
        EncounterFixture fixture = createEncounter("工作站患者");

        var queue = api.get("/api/v1/encounters/queue?date=" + LocalDate.now(), doctorToken);
        assertThat(queue.status()).isEqualTo(200);
        assertThat(queue.rawBody()).contains("工作站患者").contains("WAITING");

        var started = api.post("/api/v1/encounters/" + fixture.encounterId() + "/start", null, doctorToken);
        assertThat(started.status()).isEqualTo(200);
        assertThat(started.body().path("encounter").path("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(started.body().path("emr").path("version").asInt()).isZero();

        var saved = api.put("/api/v1/encounters/" + fixture.encounterId() + "/emr",
                Map.of("content", Map.of(
                        "chiefComplaint", "发热三天",
                        "presentIllness", "三天前受凉后发热",
                        "physicalExam", "体温 38.5℃"), "version", 0), doctorToken);
        assertThat(saved.status()).isEqualTo(200);
        assertThat(saved.body().path("version").asInt()).isEqualTo(1);
        assertThat(saved.body().path("content").path("chiefComplaint").asString()).isEqualTo("发热三天");

        var stale = api.put("/api/v1/encounters/" + fixture.encounterId() + "/emr",
                Map.of("content", Map.of("chiefComplaint", "覆盖"), "version", 0), doctorToken);
        assertThat(stale.status()).isEqualTo(409);
        assertThat(stale.body().path("code").asString()).isEqualTo("MED-STATE-002");

        var signed = api.post("/api/v1/encounters/" + fixture.encounterId() + "/emr/sign",
                Map.of("content", Map.of("chiefComplaint", "发热三天", "presentIllness", "受凉后发热",
                        "physicalExam", "体温 38.5℃"), "version", 1), doctorToken);
        assertThat(signed.status()).isEqualTo(200);
        assertThat(signed.text("status")).isEqualTo("SIGNED");
        assertThat(signed.body().path("signatureHash").asString()).hasSize(64);

        var noDiagnosis = api.post("/api/v1/encounters/" + fixture.encounterId() + "/finish", null, doctorToken);
        assertThat(noDiagnosis.status()).isEqualTo(409);

        var diagnoses = api.put("/api/v1/encounters/" + fixture.encounterId() + "/diagnoses",
                Map.of("diagnoses", List.of(Map.of(
                        "code", "J06.9", "name", "急性上呼吸道感染", "type", "PRIMARY", "note", "病毒性"))),
                doctorToken);
        assertThat(diagnoses.status()).isEqualTo(200);
        assertThat(diagnoses.rawBody()).contains("急性上呼吸道感染");

        var finished = api.post("/api/v1/encounters/" + fixture.encounterId() + "/finish", null, doctorToken);
        assertThat(finished.status()).isEqualTo(200);
        assertThat(finished.body().path("encounter").path("status").asString()).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM appointment WHERE id = ?", String.class, fixture.appointmentId()))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM encounter_event WHERE encounter_id = ?", Integer.class, fixture.encounterId()))
                .isGreaterThanOrEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE action = 'ENCOUNTER_COMPLETED' AND resource_id = ?",
                Integer.class, String.valueOf(fixture.encounterId()))).isPositive();
    }

    @Test
    void should_reject_finish_without_chief_complaint() {
        EncounterFixture fixture = createEncounter("缺主诉患者");
        api.post("/api/v1/encounters/" + fixture.encounterId() + "/start", null, doctorToken);
        var sign = api.post("/api/v1/encounters/" + fixture.encounterId() + "/emr/sign",
                Map.of("content", Map.of("chiefComplaint", ""), "version", 0), doctorToken);
        assertThat(sign.status()).isEqualTo(422);
        assertThat(sign.rawBody()).contains("chiefComplaint");
    }

    @Test
    void should_handle_concurrent_emr_saves_with_optimistic_lock() throws Exception {
        EncounterFixture fixture = createEncounter("并发病历患者");
        api.post("/api/v1/encounters/" + fixture.encounterId() + "/start", null, doctorToken);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task = () -> api.put("/api/v1/encounters/" + fixture.encounterId() + "/emr",
                    Map.of("content", Map.of("chiefComplaint", "并发"), "version", 0), doctorToken).status();
            List<Future<Integer>> futures = pool.invokeAll(List.of(task, task));
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT version FROM emr_record WHERE encounter_id = ?", Integer.class, fixture.encounterId()))
                .isEqualTo(1);
    }

    @Test
    void should_enforce_doctor_and_department_scope() {
        EncounterFixture fixture = createEncounter("范围患者");

        assertThat(api.get("/api/v1/encounters/" + fixture.encounterId(), token("doctor1")).status())
                .isEqualTo(403);
        assertThat(api.get("/api/v1/encounters/" + fixture.encounterId(), nurseToken).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/encounters/" + fixture.encounterId(), token("nurse1")).status())
                .isEqualTo(403);
        assertThat(api.get("/api/v1/encounters/" + fixture.encounterId(), cashierToken).status())
                .isEqualTo(403);
        assertThat(api.post("/api/v1/encounters/" + fixture.encounterId() + "/start", null,
                token("doctor1")).status()).isEqualTo(403);
    }

    private EncounterFixture createEncounter(String patientName) {
        long scheduleId = todaySchedule();
        var slots = api.get("/api/v1/schedules/" + scheduleId + "/slots", cashierToken);
        long slotId = -1;
        for (var slot : slots.body()) {
            if (slot.path("available").asInt() > 0) {
                slotId = slot.path("id").asLong();
                break;
            }
        }
        assertThat(slotId).as("available slot").isPositive();

        var patient = api.post("/api/v1/patients", Map.of(
                "name", patientName,
                "gender", "MALE",
                "birthDate", "1990-01-01",
                "idCardNo", "31010119900101" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "phone", "1395555" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "address", "上海", "allergyHistory", "无"), cashierToken);
        assertThat(patient.status()).isEqualTo(201);
        long patientId = patient.body().path("id").asLong();

        var booked = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", "ws-" + System.nanoTime()));
        assertThat(booked.status()).isEqualTo(201);
        long appointmentId = booked.body().path("id").asLong();

        var checkedIn = api.post("/api/v1/appointments/" + appointmentId + "/check-in", null, nurseToken);
        assertThat(checkedIn.status()).isEqualTo(200);
        long encounterId = checkedIn.body().path("encounterId").asLong();
        return new EncounterFixture(appointmentId, encounterId);
    }

    private long todaySchedule() {
        LocalDate today = LocalDate.now();
        var existing = api.get("/api/v1/schedules?date=" + today + "&doctorId=3", cashierToken);
        for (var schedule : existing.body()) {
            if ("AM".equals(schedule.path("period").asString())) {
                return schedule.path("id").asLong();
            }
        }
        var created = api.post("/api/v1/schedules", Map.of(
                "scheduleDate", today.toString(), "period", "AM",
                "startTime", "08:00", "endTime", "12:00", "room", "工作站诊室",
                "slotCount", 20, "capacityPerSlot", 1, "registrationFee", 20), doctorToken);
        assertThat(created.status()).isEqualTo(201);
        return created.body().path("id").asLong();
    }

    private String token(String username) {
        var response = api.login(username, "Medflow@2026");
        assertThat(response.status()).as("login " + username).isEqualTo(200);
        return response.text("accessToken");
    }

    private record EncounterFixture(long appointmentId, long encounterId) {
    }
}
