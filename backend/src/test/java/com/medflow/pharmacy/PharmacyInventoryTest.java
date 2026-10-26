package com.medflow.pharmacy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PharmacyInventoryTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void should_dispense_and_return_with_idempotency() {
        long prescriptionId = approvedPrescription("库存患者A", 2);
        var reservations = api.get("/api/v1/prescriptions/" + prescriptionId + "/reservations", pharmacistToken);
        assertThat(reservations.status()).isEqualTo(200);
        int reservedQty = reservations.body().get(0).path("quantity").asInt();
        long reservationId = reservations.body().get(0).path("id").asLong();
        int stockBefore = stockOfDrug(1);

        var dispensed = api.post("/api/v1/prescriptions/" + prescriptionId + "/dispense", null, pharmacistToken);
        assertThat(dispensed.status()).isEqualTo(200);
        assertThat(dispensed.body().path("header").path("status").asString()).isEqualTo("DISPENSED");
        assertThat(stockOfDrug(1)).isEqualTo(stockBefore - reservedQty);

        var repeat = api.post("/api/v1/prescriptions/" + prescriptionId + "/dispense", null, pharmacistToken);
        assertThat(repeat.status()).isEqualTo(200);
        assertThat(stockOfDrug(1)).isEqualTo(stockBefore - reservedQty);

        String key = "return-" + System.nanoTime();
        var returned = api.post("/api/v1/prescriptions/" + prescriptionId + "/returns",
                Map.of("reason", "患者退药", "lines", List.of(Map.of("reservationId", reservationId, "quantity", 1))),
                pharmacistToken, Map.of("Idempotency-Key", key));
        assertThat(returned.status()).isEqualTo(200);
        assertThat(stockOfDrug(1)).isEqualTo(stockBefore - reservedQty + 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT returned_qty FROM stock_reservation WHERE id = ?", Integer.class, reservationId))
                .isEqualTo(1);

        var replay = api.post("/api/v1/prescriptions/" + prescriptionId + "/returns",
                Map.of("reason", "患者退药", "lines", List.of(Map.of("reservationId", reservationId, "quantity", 1))),
                pharmacistToken, Map.of("Idempotency-Key", key));
        assertThat(replay.status()).isEqualTo(200);
        assertThat(stockOfDrug(1)).isEqualTo(stockBefore - reservedQty + 1);

        var over = api.post("/api/v1/prescriptions/" + prescriptionId + "/returns",
                Map.of("reason", "超退", "lines", List.of(Map.of("reservationId", reservationId, "quantity", 5))),
                pharmacistToken, Map.of("Idempotency-Key", "over-" + System.nanoTime()));
        assertThat(over.status()).isEqualTo(422);
    }

    @Test
    void should_inbound_batches_and_reject_expired_batches() {
        int before = stockOfDrug(1);
        LocalDate expiry = LocalDate.now().plusMonths(6);
        var created = api.post("/api/v1/stock/batches", Map.of(
                "drugId", 1, "batchNo", "M6-" + System.nanoTime(),
                "productionDate", LocalDate.now().minusMonths(1).toString(),
                "expiryDate", expiry.toString(), "quantity", 25), pharmacistToken);
        assertThat(created.status()).isEqualTo(200);
        assertThat(created.body().path("stockQty").asInt()).isEqualTo(25);
        assertThat(stockOfDrug(1)).isEqualTo(before + 25);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stock_movement WHERE type = 'IN' AND batch_id = ?", Integer.class,
                created.body().path("id").asLong())).isEqualTo(1);

        var expired = api.post("/api/v1/stock/batches", Map.of(
                "drugId", 1, "batchNo", "EXP-" + System.nanoTime(),
                "expiryDate", LocalDate.now().minusDays(1).toString(), "quantity", 10), pharmacistToken);
        assertThat(expired.status()).isEqualTo(422);

        var summary = api.get("/api/v1/stock/summary", pharmacistToken);
        assertThat(summary.status()).isEqualTo(200);
        assertThat(summary.rawBody()).contains("阿莫西林");
        var movements = api.get("/api/v1/stock/movements?limit=20", pharmacistToken);
        assertThat(movements.status()).isEqualTo(200);
        assertThat(movements.rawBody()).contains("IN");
    }

    @Test
    void should_quarantine_returned_expired_batch() {
        long prescriptionId = approvedPrescription("库存患者B", 1);
        var reservations = api.get("/api/v1/prescriptions/" + prescriptionId + "/reservations", pharmacistToken);
        long reservationId = reservations.body().get(0).path("id").asLong();
        long batchId = reservations.body().get(0).path("batchId").asLong();
        api.post("/api/v1/prescriptions/" + prescriptionId + "/dispense", null, pharmacistToken);

        jdbcTemplate.update("UPDATE drug_batch SET expiry_date = DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY) WHERE id = ?",
                batchId);
        var returned = api.post("/api/v1/prescriptions/" + prescriptionId + "/returns",
                Map.of("reason", "过期退药", "lines", List.of(Map.of("reservationId", reservationId, "quantity", 1))),
                pharmacistToken, Map.of("Idempotency-Key", "expired-return-" + System.nanoTime()));
        assertThat(returned.status()).isEqualTo(200);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM drug_batch WHERE id = ?", String.class, batchId))
                .isEqualTo("QUARANTINE");
    }

    @Test
    void should_manage_drug_catalog_and_permissions() {
        String code = "M6DRUG" + System.nanoTime();
        var created = api.post("/api/v1/drugs", Map.of(
                "code", code, "name", "测试药品", "genericName", "测试通用名", "spec", "10mg*10片",
                "unit", "盒", "dosageForm", "片剂", "manufacturer", "合成药厂", "category", "WESTERN",
                "unitPrice", 9.90, "prescriptionType", "RX"), pharmacistToken);
        assertThat(created.status()).isEqualTo(200);
        long drugId = created.body().path("id").asLong();

        assertThat(api.post("/api/v1/drugs", Map.of(
                "code", code, "name", "重复", "spec", "1", "unit", "盒", "unitPrice", 1), pharmacistToken)
                .status()).isEqualTo(409);

        var disabled = api.post("/api/v1/drugs/" + drugId + "/status",
                Map.of("status", "DISABLED"), pharmacistToken);
        assertThat(disabled.status()).isEqualTo(200);
        assertThat(disabled.body().path("status").asString()).isEqualTo("DISABLED");
        assertThat(api.get("/api/v1/drugs", pharmacistToken).rawBody()).doesNotContain(code);

        assertThat(api.get("/api/v1/drugs/admin", doctorToken).status()).isEqualTo(403);
        assertThat(api.post("/api/v1/stock/batches", Map.of(
                "drugId", 1, "batchNo", "NOAUTH", "expiryDate", LocalDate.now().plusDays(1).toString(),
                "quantity", 1), doctorToken).status()).isEqualTo(403);
    }

    private int stockOfDrug(long drugId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(stock_qty),0) FROM drug_batch WHERE drug_id = ?", Integer.class, drugId);
    }

    private long approvedPrescription(String patientName, int quantity) {
        long encounterId = createEncounter(patientName);
        api.post("/api/v1/encounters/" + encounterId + "/start", null, doctorToken);
        var created = api.post("/api/v1/prescriptions/encounters/" + encounterId, Map.of(
                "type", "WESTERN", "diagnosisSummary", "上感", "medicalAdvice", "口服",
                "items", List.of(Map.of("drugId", 1, "dosage", "1粒", "frequency", "每日3次",
                        "route", "口服", "days", 3, "quantity", quantity))), doctorToken);
        assertThat(created.status()).isEqualTo(200);
        long id = created.body().path("header").path("id").asLong();
        var submitted = api.post("/api/v1/prescriptions/" + id + "/submit",
                Map.of("version", created.body().path("header").path("version").asInt()), doctorToken,
                Map.of("Idempotency-Key", "m6-submit-" + System.nanoTime()));
        assertThat(submitted.status()).isEqualTo(200);
        var started = api.post("/api/v1/prescriptions/" + id + "/review/start",
                Map.of("version", submitted.body().path("header").path("version").asInt()), pharmacistToken);
        var approved = api.post("/api/v1/prescriptions/" + id + "/review/approve",
                Map.of("version", started.body().path("header").path("version").asInt()), pharmacistToken);
        assertThat(approved.status()).isEqualTo(200);
        return id;
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
                    "startTime", "08:00", "endTime", "12:00", "room", "库存诊室",
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
                "name", patientName, "gender", "FEMALE", "birthDate", "1991-01-01",
                "idCardNo", "31010119910101" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "phone", "1398888" + String.format("%04d", Math.abs(patientName.hashCode()) % 10000),
                "address", "上海", "allergyHistory", "无"), cashierToken);
        assertThat(patient.status()).isEqualTo(201);
        long patientId = patient.body().path("id").asLong();
        var booked = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", "m6-enc-" + System.nanoTime()));
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
