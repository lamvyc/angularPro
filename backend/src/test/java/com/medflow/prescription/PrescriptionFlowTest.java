package com.medflow.prescription;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.medflow.AbstractIntegrationTest;
import com.medflow.prescription.application.PrescriptionTxService;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionFlowTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrescriptionTxService prescriptionTxService;

    private ApiTestClient api;
    private String doctorToken;
    private String pharmacistToken;
    private String cashierToken;
    private String nurseToken;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        doctorToken = token("doctor2");
        pharmacistToken = token("pharmacist1");
        cashierToken = token("cashier1");
        nurseToken = token("nurse3");
    }

    @Test
    void should_submit_review_reserve_and_cancel_release() {
        long encounterId = createEncounter("处方患者A");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var drugs = api.get("/api/v1/drugs", doctorToken);
        assertThat(drugs.status()).isEqualTo(200);
        assertThat(drugs.rawBody()).contains("阿莫西林");

        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(1, 2), doctorToken);
        assertThat(created.status()).isEqualTo(200);
        long id = created.body().path("header").path("id").asLong();
        assertThat(created.body().path("header").path("status").asString()).isEqualTo("DRAFT");
        int version = created.body().path("header").path("version").asInt();

        String key = "rx-submit-" + System.nanoTime();
        var submitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", version), doctorToken, Map.of("Idempotency-Key", key));
        assertThat(submitted.status()).isEqualTo(200);
        assertThat(submitted.body().path("header").path("status").asString()).isEqualTo("SUBMITTED");
        assertThat(submitted.body().path("header").path("revisionNo").asInt()).isEqualTo(1);

        var replay = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", version), doctorToken, Map.of("Idempotency-Key", key));
        assertThat(replay.status()).isEqualTo(200);
        assertThat(replay.body().path("header").path("revisionNo").asInt()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prescription_revision WHERE prescription_id = ?", Integer.class, id))
                .isEqualTo(1);

        int submittedVersion = submitted.body().path("header").path("version").asInt();
        var started = api.post("/api/v1/prescriptions/" + id + "/review/start",
                Map.of("version", submittedVersion), pharmacistToken);
        assertThat(started.status()).isEqualTo(200);
        assertThat(started.body().path("header").path("status").asString()).isEqualTo("UNDER_REVIEW");

        int reviewVersion = started.body().path("header").path("version").asInt();
        var approved = api.post("/api/v1/prescriptions/" + id + "/review/approve",
                Map.of("version", reviewVersion), pharmacistToken);
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.body().path("header").path("status").asString()).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE prescription_id = ? AND status = 'HELD'",
                Integer.class, id)).isGreaterThan(0);
        Integer reserved = jdbcTemplate.queryForObject(
                "SELECT SUM(reserved_qty) FROM drug_batch WHERE drug_id = 1", Integer.class);
        assertThat(reserved).isGreaterThanOrEqualTo(2);

        int approvedVersion = approved.body().path("header").path("version").asInt();
        var cancelled = api.post("/api/v1/prescriptions/" + id + "/cancel",
                Map.of("version", approvedVersion, "reason", "患者取消用药"), doctorToken);
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.body().path("header").path("status").asString()).isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE prescription_id = ? AND status = 'HELD'",
                Integer.class, id)).isZero();
        Integer after = jdbcTemplate.queryForObject(
                "SELECT SUM(reserved_qty) FROM drug_batch WHERE drug_id = 1", Integer.class);
        assertThat(after).isZero();
    }

    @Test
    void should_reject_then_resubmit_with_new_revision() {
        long encounterId = createEncounter("处方患者B");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(2, 1), doctorToken);
        long id = created.body().path("header").path("id").asLong();
        var submitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", created.body().path("header").path("version").asInt()), doctorToken,
                Map.of("Idempotency-Key", "rx-b-" + System.nanoTime()));
        int version = submitted.body().path("header").path("version").asInt();
        var started = api.post("/api/v1/prescriptions/" + id + "/review/start",
                Map.of("version", version), pharmacistToken);
        int reviewVersion = started.body().path("header").path("version").asInt();
        var rejected = api.post("/api/v1/prescriptions/" + id + "/review/reject",
                Map.of("version", reviewVersion, "reason", "用法用量不清晰"), pharmacistToken);
        assertThat(rejected.status()).isEqualTo(200);
        assertThat(rejected.body().path("header").path("status").asString()).isEqualTo("REJECTED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE prescription_id = ?", Integer.class, id)).isZero();

        int rejectedVersion = rejected.body().path("header").path("version").asInt();
        var updated = api.put("/api/v1/prescriptions/" + id,
                prescriptionBodyWithNote(2, 1, "饭后服用"), doctorToken);
        assertThat(updated.status()).isEqualTo(200);
        int updatedVersion = updated.body().path("header").path("version").asInt();
        var resubmitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", updatedVersion), doctorToken,
                Map.of("Idempotency-Key", "rx-b2-" + System.nanoTime()));
        assertThat(resubmitted.status()).isEqualTo(200);
        assertThat(resubmitted.body().path("header").path("revisionNo").asInt()).isEqualTo(2);
        assertThat(resubmitted.body().path("header").path("status").asString()).isEqualTo("SUBMITTED");
        assertThat(rejectedVersion).isPositive();
    }

    @Test
    void should_keep_under_review_when_stock_is_insufficient() {
        long encounterId = createEncounter("处方患者C");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(3, 999), doctorToken);
        long id = created.body().path("header").path("id").asLong();
        var submitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", created.body().path("header").path("version").asInt()), doctorToken,
                Map.of("Idempotency-Key", "rx-c-" + System.nanoTime()));
        var started = api.post("/api/v1/prescriptions/" + id + "/review/start",
                Map.of("version", submitted.body().path("header").path("version").asInt()), pharmacistToken);
        var approved = api.post("/api/v1/prescriptions/" + id + "/review/approve",
                Map.of("version", started.body().path("header").path("version").asInt()), pharmacistToken);
        assertThat(approved.status()).isEqualTo(409);
        assertThat(approved.body().path("code").asString()).isEqualTo("MED-STOCK-001");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM prescription WHERE id = ?", String.class, id)).isEqualTo("UNDER_REVIEW");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE prescription_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void should_expire_approved_prescription_and_release_stock() {
        long encounterId = createEncounter("处方患者D");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(1, 1), doctorToken);
        long id = created.body().path("header").path("id").asLong();
        var submitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", created.body().path("header").path("version").asInt()), doctorToken,
                Map.of("Idempotency-Key", "rx-d-" + System.nanoTime()));
        var started = api.post("/api/v1/prescriptions/" + id + "/review/start",
                Map.of("version", submitted.body().path("header").path("version").asInt()), pharmacistToken);
        var approved = api.post("/api/v1/prescriptions/" + id + "/review/approve",
                Map.of("version", started.body().path("header").path("version").asInt()), pharmacistToken);
        assertThat(approved.status()).isEqualTo(200);

        jdbcTemplate.update("UPDATE prescription SET expires_at = DATE_SUB(NOW(3), INTERVAL 1 HOUR) WHERE id = ?", id);
        prescriptionTxService.expireDue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM prescription WHERE id = ?", String.class, id)).isEqualTo("EXPIRED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE prescription_id = ? AND status = 'HELD'",
                Integer.class, id)).isZero();
    }

    @Test
    void should_enforce_role_and_doctor_scope() {
        long encounterId = createEncounter("处方患者E");
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(1, 1), doctorToken);
        long id = created.body().path("header").path("id").asLong();

        assertThat(api.post("/api/v1/prescriptions/encounters/" + encounterId,
                prescriptionBody(1, 1), pharmacistToken).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/prescriptions/" + id, token("doctor1")).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/prescriptions/" + id, cashierToken).status()).isEqualTo(403);
        var list = api.get("/api/v1/prescriptions?encounterId=" + encounterId, pharmacistToken);
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.rawBody()).contains("处方患者E");
    }

    private Map<String, Object> prescriptionBody(long drugId, int quantity) {
        return prescriptionBodyWithNote(drugId, quantity, null);
    }

    private Map<String, Object> prescriptionBodyWithNote(long drugId, int quantity, String note) {
        return Map.of(
                "type", "WESTERN",
                "diagnosisSummary", "急性上呼吸道感染",
                "medicalAdvice", "饭后服用",
                "items", List.of(Map.of(
                        "drugId", drugId,
                        "dosage", "1粒",
                        "frequency", "每日3次",
                        "route", "口服",
                        "days", 3,
                        "quantity", quantity,
                        "note", note == null ? "" : note)));
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
                    "startTime", "08:00", "endTime", "12:00", "room", "处方诊室",
                    "slotCount", 30, "capacityPerSlot", 1, "registrationFee", 20), doctorToken);
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
                "name", patientName, "gender", "FEMALE", "birthDate", "1990-01-01",
                "idCardNo", "31010119900101" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "phone", "1397777" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "address", "上海", "allergyHistory", "无"), cashierToken);
        assertThat(patient.status()).isEqualTo(201);
        long patientId = patient.body().path("id").asLong();
        var booked = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", "rx-enc-" + System.nanoTime()));
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
