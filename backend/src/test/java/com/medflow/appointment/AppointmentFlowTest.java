package com.medflow.appointment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.medflow.AbstractIntegrationTest;
import com.medflow.support.ApiTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentFlowTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ApiTestClient api;
    private String doctor2Token;
    private String cashierToken;
    private String nurseToken;
    private String doctor1Token;

    @BeforeEach
    void setUp() {
        api = new ApiTestClient(port);
        doctor2Token = token("doctor2");
        doctor1Token = token("doctor1");
        cashierToken = token("cashier1");
        nurseToken = token("nurse3");
    }

    @Test
    void should_create_patient_and_schedule_with_slots() {
        LocalDate date = LocalDate.now().plusDays(1);
        long scheduleId = createSchedule(doctor2Token, date, "AM", 4, 1);

        var slots = api.get("/api/v1/schedules/" + scheduleId + "/slots", cashierToken);
        assertThat(slots.status()).isEqualTo(200);
        assertThat(slots.body().size()).isEqualTo(4);
        assertThat(slots.body().get(0).path("bookedCount").asInt()).isZero();

        var schedules = api.get("/api/v1/schedules?date=" + date, cashierToken);
        assertThat(schedules.status()).isEqualTo(200);
        assertThat(schedules.rawBody()).contains("availableCount");
    }

    @Test
    void should_book_with_idempotency_and_reject_duplicate() {
        LocalDate date = LocalDate.now().plusDays(2);
        long scheduleId = createSchedule(doctor2Token, date, "AM", 2, 1);
        long slotId = firstSlot(scheduleId);
        long patientId = createPatient("幂等患者", "13910000001");

        String key = "book-" + System.nanoTime();
        var first = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", key));
        assertThat(first.status()).isEqualTo(201);
        String appointmentNo = first.text("appointmentNo");

        var replay = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", key));
        assertThat(replay.status()).isEqualTo(200);
        assertThat(replay.text("appointmentNo")).isEqualTo(appointmentNo);

        var duplicate = api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", "book-other-" + System.nanoTime()));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.body().path("code").asString()).isEqualTo("MED-SLOT-002");

        Integer booked = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM schedule_slot WHERE id = ?", Integer.class, slotId);
        assertThat(booked).isEqualTo(1);
    }

    @Test
    void should_not_oversell_under_fifty_concurrent_bookings() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        long scheduleId = createSchedule(doctor2Token, date, "PM", 1, 1);
        long slotId = firstSlot(scheduleId);

        List<Long> patientIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            patientIds.add(createPatient("并发患者" + i, "1392000" + String.format("%04d", i)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(24);
        AtomicInteger created = new AtomicInteger();
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                long patientId = patientIds.get(i);
                String key = "concurrent-" + i + "-" + System.nanoTime();
                tasks.add(() -> {
                    var response = api.post("/api/v1/appointments",
                            Map.of("patientId", patientId, "slotId", slotId),
                            cashierToken, Map.of("Idempotency-Key", key));
                    if (response.status() == 201) {
                        created.incrementAndGet();
                    }
                    return response.status();
                });
            }
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(created.get()).isEqualTo(1);
        Integer booked = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM schedule_slot WHERE id = ?", Integer.class, slotId);
        assertThat(booked).isEqualTo(1);
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointment WHERE slot_id = ?", Integer.class, slotId);
        assertThat(total).isEqualTo(1);
    }

    @Test
    void should_cancel_release_slot_and_refund_paid_charge() {
        LocalDate date = LocalDate.now().plusDays(4);
        long scheduleId = createSchedule(doctor2Token, date, "AM", 2, 1);
        long slotId = firstSlot(scheduleId);
        long patientId = createPatient("退号患者", "13930000001");
        var booked = book(patientId, slotId, "cancel-" + System.nanoTime());
        assertThat(booked.status()).isEqualTo(201);
        long appointmentId = booked.body().path("id").asLong();
        long chargeId = booked.body().path("chargeId").asLong();

        assertThat(api.post("/api/v1/charges/" + chargeId + "/pay",
                Map.of("paymentMethod", "CASH"), cashierToken).status()).isEqualTo(200);

        var cancelled = api.post("/api/v1/appointments/" + appointmentId + "/cancel",
                Map.of("reason", "患者临时有事"), cashierToken);
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.text("status")).isEqualTo("CANCELLED");

        Integer bookedCount = jdbcTemplate.queryForObject(
                "SELECT booked_count FROM schedule_slot WHERE id = ?", Integer.class, slotId);
        assertThat(bookedCount).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM charge_order WHERE id = ?", String.class, chargeId)).isEqualTo("REFUNDED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM refund_record WHERE charge_id = ?", String.class, chargeId)).isEqualTo("SUCCESS");
    }

    @Test
    void should_check_in_and_create_waiting_encounter() {
        LocalDate date = LocalDate.now();
        long scheduleId = createSchedule(doctor2Token, date, "PM", 2, 1);
        long slotId = firstSlot(scheduleId);
        long patientId = createPatient("签到患者", "13940000001");
        var booked = book(patientId, slotId, "checkin-" + System.nanoTime());
        assertThat(booked.status()).isEqualTo(201);
        long appointmentId = booked.body().path("id").asLong();

        var checkedIn = api.post("/api/v1/appointments/" + appointmentId + "/check-in", null, nurseToken);
        assertThat(checkedIn.status()).isEqualTo(200);
        assertThat(checkedIn.text("status")).isEqualTo("CHECKED_IN");
        assertThat(checkedIn.body().path("encounterId").asLong()).isPositive();

        Integer encounters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM encounter WHERE appointment_id = ?", Integer.class, appointmentId);
        assertThat(encounters).isEqualTo(1);

        assertThat(api.post("/api/v1/appointments/" + appointmentId + "/check-in", null, nurseToken).status())
                .isEqualTo(409);
    }

    @Test
    void should_enforce_role_and_department_scope() {
        LocalDate date = LocalDate.now().plusDays(5);
        createSchedule(doctor2Token, date, "AM", 2, 1);

        var doctor2Appointments = api.get("/api/v1/appointments?date=" + date, doctor2Token);
        assertThat(doctor2Appointments.status()).isEqualTo(200);

        var doctor1Appointments = api.get("/api/v1/appointments?date=" + date, doctor1Token);
        assertThat(doctor1Appointments.status()).isEqualTo(200);
        assertThat(doctor1Appointments.rawBody()).doesNotContain("doctor2");

        assertThat(api.get("/api/v1/appointments", token("pharmacist1")).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/appointments", token("admin")).status()).isEqualTo(403);
        assertThat(api.get("/api/v1/appointments", null).status()).isEqualTo(401);

        assertThat(api.post("/api/v1/schedules", Map.of(
                "scheduleDate", date.toString(), "period", "PM",
                "startTime", "14:00", "endTime", "17:00",
                "slotCount", 2, "capacityPerSlot", 1, "registrationFee", 20),
                cashierToken).status()).isEqualTo(403);
    }

    private String token(String username) {
        var response = api.login(username, "Medflow@2026");
        assertThat(response.status()).as("login " + username).isEqualTo(200);
        return response.text("accessToken");
    }

    private long createSchedule(String token, LocalDate date, String period, int slotCount, int capacity) {
        var response = api.post("/api/v1/schedules", Map.of(
                "scheduleDate", date.toString(),
                "period", period,
                "startTime", period.equals("AM") ? "08:00" : "14:00",
                "endTime", period.equals("AM") ? "12:00" : "17:00",
                "room", "诊室 1",
                "slotCount", slotCount,
                "capacityPerSlot", capacity,
                "registrationFee", 20), token);
        assertThat(response.status()).as("create schedule %s %s", date, period).isEqualTo(201);
        return response.body().path("id").asLong();
    }

    private long firstSlot(long scheduleId) {
        var response = api.get("/api/v1/schedules/" + scheduleId + "/slots", cashierToken);
        assertThat(response.status()).isEqualTo(200);
        return response.body().get(0).path("id").asLong();
    }

    private long createPatient(String name, String phone) {
        var response = api.post("/api/v1/patients", Map.of(
                "name", name,
                "gender", "FEMALE",
                "birthDate", "1990-01-01",
                "idCardNo", "31010119900101" + String.format("%04d", Math.abs(phone.hashCode()) % 10000),
                "phone", phone,
                "address", "上海合成地址",
                "allergyHistory", "无"), cashierToken);
        assertThat(response.status()).as("create patient " + name).isEqualTo(201);
        return response.body().path("id").asLong();
    }

    private ApiTestClient.Response book(long patientId, long slotId, String key) {
        return api.post("/api/v1/appointments", Map.of("patientId", patientId, "slotId", slotId),
                cashierToken, Map.of("Idempotency-Key", key));
    }
}
