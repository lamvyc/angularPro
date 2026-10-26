package com.medflow.appointment.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.medflow.appointment.domain.AppointmentView;
import com.medflow.appointment.domain.ChargeView;
import com.medflow.appointment.domain.DoctorSchedule;
import com.medflow.appointment.domain.IdemRecord;
import com.medflow.appointment.domain.NewAppointment;
import com.medflow.appointment.domain.NewCharge;
import com.medflow.appointment.domain.NewEncounter;
import com.medflow.appointment.domain.NewPatient;
import com.medflow.appointment.domain.NewRefund;
import com.medflow.appointment.domain.NewSchedule;
import com.medflow.appointment.domain.NewSlot;
import com.medflow.appointment.domain.Patient;
import com.medflow.appointment.domain.ScheduleSlot;
import com.medflow.appointment.domain.SlotBookingInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AppointmentMapper {

    // ---------------- patient ----------------

    @Select("""
            SELECT id, patient_no AS patientNo, name, gender, birth_date AS birthDate,
                   id_card_masked AS idCardMasked, phone, address, allergy_history AS allergyHistory, status
            FROM patient WHERE id = #{id}
            """)
    Optional<Patient> findPatient(long id);

    @Select("""
            SELECT id, patient_no AS patientNo, name, gender, birth_date AS birthDate,
                   id_card_masked AS idCardMasked, phone, address, allergy_history AS allergyHistory, status
            FROM patient WHERE patient_no = #{patientNo}
            """)
    Optional<Patient> findPatientByNo(String patientNo);

    @Select("""
            <script>
            SELECT id, patient_no AS patientNo, name, gender, birth_date AS birthDate,
                   id_card_masked AS idCardMasked, phone, address, allergy_history AS allergyHistory, status
            FROM patient
            <where>
              <if test="keyword != null and keyword != ''">
                AND (name LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%')
                  OR patient_no LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY id DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Patient> searchPatients(@Param("keyword") String keyword, @Param("offset") int offset,
            @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM patient
            <where>
              <if test="keyword != null and keyword != ''">
                AND (name LIKE CONCAT('%', #{keyword}, '%')
                  OR phone LIKE CONCAT('%', #{keyword}, '%')
                  OR patient_no LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            </script>
            """)
    long countPatients(String keyword);

    @Select("SELECT COUNT(*) FROM patient WHERE id_card_hash = #{hash}")
    int countIdCardHash(String hash);

    @Insert("""
            INSERT INTO patient (patient_no, name, gender, birth_date, id_card_hash, id_card_masked,
                                 phone, address, allergy_history, status, created_by)
            VALUES (#{patientNo}, #{name}, #{gender}, #{birthDate}, #{idCardHash}, #{idCardMasked},
                    #{phone}, #{address}, #{allergyHistory}, 'ACTIVE', #{operatorId})
            """)
    int insertPatient(NewPatient patient);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    // ---------------- schedule ----------------

    @Insert("""
            INSERT INTO doctor_schedule (dept_id, doctor_id, schedule_date, period, start_time, end_time,
                                         room, registration_fee, status, published_at, published_by, created_by)
            VALUES (#{deptId}, #{doctorId}, #{scheduleDate}, #{period}, #{startTime}, #{endTime},
                    #{room}, #{registrationFee}, 'PUBLISHED', #{publishedAt}, #{publishedBy}, #{publishedBy})
            """)
    int insertSchedule(NewSchedule schedule);

    @Insert("""
            INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
            VALUES (#{scheduleId}, #{seqNo}, #{startTime}, #{endTime}, #{capacity}, 'OPEN')
            """)
    int insertSlot(NewSlot slot);

    @Select("""
            SELECT s.id, s.dept_id AS deptId, d.name AS deptName, s.doctor_id AS doctorId,
                   u.real_name AS doctorName, s.schedule_date AS scheduleDate, s.period,
                   s.start_time AS startTime, s.end_time AS endTime, s.room,
                   s.registration_fee AS registrationFee, s.status,
                   COALESCE(SUM(sl.capacity - sl.booked_count), 0) AS availableCount
            FROM doctor_schedule s
            JOIN sys_department d ON d.id = s.dept_id
            JOIN sys_user u ON u.id = s.doctor_id
            LEFT JOIN schedule_slot sl ON sl.schedule_id = s.id
            WHERE s.id = #{id}
            GROUP BY s.id, d.name, u.real_name
            """)
    Optional<DoctorSchedule> findSchedule(long id);

    @Select("""
            <script>
            SELECT s.id, s.dept_id AS deptId, d.name AS deptName, s.doctor_id AS doctorId,
                   u.real_name AS doctorName, s.schedule_date AS scheduleDate, s.period,
                   s.start_time AS startTime, s.end_time AS endTime, s.room,
                   s.registration_fee AS registrationFee, s.status,
                   COALESCE(SUM(sl.capacity - sl.booked_count), 0) AS availableCount
            FROM doctor_schedule s
            JOIN sys_department d ON d.id = s.dept_id
            JOIN sys_user u ON u.id = s.doctor_id
            LEFT JOIN schedule_slot sl ON sl.schedule_id = s.id
            <where>
              <if test="date != null">AND s.schedule_date = #{date}</if>
              <if test="deptId != null">AND s.dept_id = #{deptId}</if>
              <if test="doctorId != null">AND s.doctor_id = #{doctorId}</if>
              AND s.status IN ('PUBLISHED', 'CLOSED')
            </where>
            GROUP BY s.id, d.name, u.real_name
            ORDER BY s.schedule_date, s.start_time
            </script>
            """)
    List<DoctorSchedule> listSchedules(@Param("date") LocalDate date, @Param("deptId") Long deptId,
            @Param("doctorId") Long doctorId);

    @Select("""
            SELECT id, schedule_id AS scheduleId, seq_no AS seqNo, start_time AS startTime,
                   end_time AS endTime, capacity, booked_count AS bookedCount,
                   capacity - booked_count AS available, status
            FROM schedule_slot WHERE schedule_id = #{scheduleId} ORDER BY seq_no
            """)
    List<ScheduleSlot> listSlots(long scheduleId);

    @Update("UPDATE doctor_schedule SET status = 'CLOSED', updated_at = NOW(3) WHERE id = #{id} AND doctor_id = #{doctorId}")
    int closeSchedule(@Param("id") long id, @Param("doctorId") long doctorId);

    @Update("UPDATE schedule_slot SET status = 'CLOSED', updated_at = NOW(3) WHERE schedule_id = #{scheduleId} AND status = 'OPEN'")
    int closeSlots(long scheduleId);

    @Select("""
            SELECT sl.id AS slotId, sl.schedule_id AS scheduleId, sl.status AS slotStatus,
                   sl.capacity, sl.booked_count AS bookedCount, s.status AS scheduleStatus,
                   s.schedule_date AS scheduleDate, s.period, s.doctor_id AS doctorId,
                   s.dept_id AS deptId, s.registration_fee AS registrationFee, sl.start_time AS startTime
            FROM schedule_slot sl JOIN doctor_schedule s ON s.id = sl.schedule_id
            WHERE sl.id = #{slotId}
            """)
    Optional<SlotBookingInfo> findSlotBooking(long slotId);

    @Select("""
            SELECT COUNT(*) FROM appointment
            WHERE slot_id = #{slotId} AND patient_id = #{patientId}
              AND status IN ('BOOKED', 'CHECKED_IN', 'IN_CONSULTATION')
            """)
    int countActiveAppointment(@Param("slotId") long slotId, @Param("patientId") long patientId);

    @Update("""
            UPDATE schedule_slot
            SET booked_count = booked_count + 1, version = version + 1, updated_at = NOW(3)
            WHERE id = #{slotId} AND status = 'OPEN' AND booked_count < capacity
            """)
    int reserveSlot(long slotId);

    @Update("""
            UPDATE schedule_slot
            SET booked_count = booked_count - 1, version = version + 1, updated_at = NOW(3)
            WHERE id = #{slotId} AND booked_count > 0
            """)
    int releaseSlot(long slotId);

    // ---------------- appointment ----------------

    @Insert("""
            INSERT INTO appointment (appointment_no, patient_id, dept_id, doctor_id, schedule_id, slot_id,
                                     appointment_date, period, seq_no, status, source, booked_by, created_by)
            VALUES (#{appointmentNo}, #{patientId}, #{deptId}, #{doctorId}, #{scheduleId}, #{slotId},
                    #{appointmentDate}, #{period}, #{seqNo}, #{status}, #{source}, #{bookedBy}, #{bookedBy})
            """)
    int insertAppointment(NewAppointment appointment);

    @Update("UPDATE doctor_schedule SET next_seq = LAST_INSERT_ID(next_seq + 1) WHERE id = #{scheduleId}")
    int bumpScheduleSeq(long scheduleId);

    @Select("""
            SELECT
                   a.id, a.appointment_no AS appointmentNo, a.status, a.patient_id AS patientId,
                   p.name AS patientName, a.dept_id AS deptId, d.name AS deptName, a.doctor_id AS doctorId,
                   u.real_name AS doctorName, a.schedule_id AS scheduleId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate, a.period, a.seq_no AS seqNo,
                   a.encounter_id AS encounterId, c.id AS chargeId, c.status AS chargeStatus,
                   c.amount AS chargeAmount, a.created_at AS createdAt
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            JOIN sys_department d ON d.id = a.dept_id
            JOIN sys_user u ON u.id = a.doctor_id
            LEFT JOIN charge_order c ON c.biz_type = 'REGISTRATION' AND c.biz_id = a.id
            WHERE a.id = #{id}
            """)
    Optional<AppointmentView> findAppointment(long id);

    @Select("""
            <script>
            SELECT
                   a.id, a.appointment_no AS appointmentNo, a.status, a.patient_id AS patientId,
                   p.name AS patientName, a.dept_id AS deptId, d.name AS deptName, a.doctor_id AS doctorId,
                   u.real_name AS doctorName, a.schedule_id AS scheduleId, a.slot_id AS slotId,
                   a.appointment_date AS appointmentDate, a.period, a.seq_no AS seqNo,
                   a.encounter_id AS encounterId, c.id AS chargeId, c.status AS chargeStatus,
                   c.amount AS chargeAmount, a.created_at AS createdAt
            FROM appointment a
            JOIN patient p ON p.id = a.patient_id
            JOIN sys_department d ON d.id = a.dept_id
            JOIN sys_user u ON u.id = a.doctor_id
            LEFT JOIN charge_order c ON c.biz_type = 'REGISTRATION' AND c.biz_id = a.id
            <where>
              <if test="date != null">AND a.appointment_date = #{date}</if>
              <if test="status != null and status != ''">AND a.status = #{status}</if>
              <if test="deptId != null">AND a.dept_id = #{deptId}</if>
              <if test="doctorId != null">AND a.doctor_id = #{doctorId}</if>
            </where>
            ORDER BY a.appointment_date, a.seq_no, a.id
            </script>
            """)
    List<AppointmentView> listAppointments(@Param("date") LocalDate date, @Param("status") String status,
            @Param("deptId") Long deptId, @Param("doctorId") Long doctorId);

    @Update("""
            UPDATE appointment
            SET status = #{toStatus},
                checked_in_at = CASE WHEN #{toStatus} = 'CHECKED_IN' THEN #{at} ELSE checked_in_at END,
                cancelled_at = CASE WHEN #{toStatus} = 'CANCELLED' THEN #{at} ELSE cancelled_at END,
                cancel_reason = CASE WHEN #{toStatus} = 'CANCELLED' THEN #{reason} ELSE cancel_reason END,
                updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND status = #{fromStatus}
            """)
    int transitionAppointment(@Param("id") long id, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("at") Instant at, @Param("reason") String reason);

    @Update("UPDATE appointment SET encounter_id = #{encounterId}, updated_at = NOW(3) WHERE id = #{appointmentId}")
    int updateEncounterId(@Param("appointmentId") long appointmentId, @Param("encounterId") long encounterId);

    @Update("""
            UPDATE appointment SET status = 'NO_SHOW', updated_at = NOW(3), version = version + 1
            WHERE appointment_date < #{date} AND status IN ('BOOKED', 'CHECKED_IN')
            """)
    int markNoShow(LocalDate date);

    // ---------------- charge / refund ----------------

    @Insert("""
            INSERT INTO charge_order (order_no, biz_type, biz_id, patient_id, dept_id, amount, status, created_by)
            VALUES (#{orderNo}, #{bizType}, #{bizId}, #{patientId}, #{deptId}, #{amount}, #{status}, #{operatorId})
            """)
    int insertCharge(NewCharge charge);

    @Select("""
            SELECT id, order_no AS orderNo, biz_type AS bizType, biz_id AS bizId, patient_id AS patientId,
                   dept_id AS deptId, amount, status, payment_method AS paymentMethod, paid_at AS paidAt,
                   cashier_id AS cashierId
            FROM charge_order WHERE id = #{id}
            """)
    Optional<ChargeView> findCharge(long id);

    @Select("""
            SELECT id, order_no AS orderNo, biz_type AS bizType, biz_id AS bizId, patient_id AS patientId,
                   dept_id AS deptId, amount, status, payment_method AS paymentMethod, paid_at AS paidAt,
                   cashier_id AS cashierId
            FROM charge_order WHERE biz_type = #{bizType} AND biz_id = #{bizId}
            """)
    Optional<ChargeView> findChargeByBiz(@Param("bizType") String bizType, @Param("bizId") long bizId);

    @Update("""
            UPDATE charge_order
            SET status = 'PAID', payment_method = #{paymentMethod}, paid_at = #{at}, cashier_id = #{cashierId},
                updated_by = #{cashierId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND status = 'UNPAID'
            """)
    int markChargePaid(@Param("id") long id, @Param("paymentMethod") String paymentMethod,
            @Param("at") Instant at, @Param("cashierId") long cashierId);

    @Update("""
            UPDATE charge_order SET status = #{status}, updated_by = #{operatorId}, updated_at = NOW(3),
                version = version + 1
            WHERE id = #{id}
            """)
    int markChargeStatus(@Param("id") long id, @Param("status") String status,
            @Param("operatorId") Long operatorId);

    @Insert("""
            INSERT INTO refund_record (refund_no, charge_id, biz_type, biz_id, amount, reason, status,
                                       operator_id, refunded_at, idem_key)
            VALUES (#{refundNo}, #{chargeId}, #{bizType}, #{bizId}, #{amount}, #{reason}, #{status},
                    #{operatorId}, #{refundedAt}, #{idemKey})
            """)
    int insertRefund(NewRefund refund);

    @Select("SELECT charge_id FROM refund_record WHERE charge_id = #{chargeId} LIMIT 1")
    Optional<Long> findRefundChargeId(long chargeId);

    // ---------------- encounter ----------------

    @Insert("""
            INSERT INTO encounter (encounter_no, appointment_id, patient_id, dept_id, doctor_id, status, created_by)
            VALUES (#{encounterNo}, #{appointmentId}, #{patientId}, #{deptId}, #{doctorId}, #{status}, #{operatorId})
            """)
    int insertEncounter(NewEncounter encounter);

    @Select("SELECT id FROM encounter WHERE appointment_id = #{appointmentId}")
    Optional<Long> findEncounterId(long appointmentId);

    // ---------------- sequence / idempotency ----------------

    @Insert("""
            INSERT INTO sys_sequence (name, next_value) VALUES (#{name}, 1)
            ON DUPLICATE KEY UPDATE next_value = LAST_INSERT_ID(next_value + 1)
            """)
    int bumpSequence(String name);

    @Insert("""
            INSERT INTO sys_idempotency_record (idem_key, user_id, method, path, request_hash, status, expires_at)
            VALUES (#{key}, #{userId}, #{method}, #{path}, #{requestHash}, #{status}, #{expiresAt})
            """)
    int insertIdempotency(@Param("userId") long userId, @Param("key") String key,
            @Param("method") String method, @Param("path") String path,
            @Param("requestHash") String requestHash, @Param("status") String status,
            @Param("expiresAt") Instant expiresAt);

    @Select("""
            SELECT status, request_hash AS requestHash, response_status AS responseStatus,
                   response_body AS responseBody, error_code AS errorCode
            FROM sys_idempotency_record WHERE user_id = #{userId} AND idem_key = #{key}
            """)
    Optional<IdemRecord> findIdempotency(@Param("userId") long userId, @Param("key") String key);

    @Update("""
            UPDATE sys_idempotency_record
            SET status = 'COMPLETED', response_status = #{responseStatus}, response_body = #{responseBody},
                updated_at = NOW(3)
            WHERE user_id = #{userId} AND idem_key = #{key}
            """)
    int completeIdempotency(@Param("userId") long userId, @Param("key") String key,
            @Param("responseStatus") int responseStatus, @Param("responseBody") String responseBody);

    @Update("""
            UPDATE sys_idempotency_record
            SET status = 'FAILED', error_code = #{errorCode}, updated_at = NOW(3)
            WHERE user_id = #{userId} AND idem_key = #{key}
            """)
    int failIdempotency(@Param("userId") long userId, @Param("key") String key,
            @Param("errorCode") String errorCode);
}
