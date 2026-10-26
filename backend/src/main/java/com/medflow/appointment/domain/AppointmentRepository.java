package com.medflow.appointment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    Optional<Patient> findPatient(long id);

    Optional<Patient> findPatientByNo(String patientNo);

    List<Patient> searchPatients(String keyword, int offset, int size);

    long countPatients(String keyword);

    boolean existsIdCardHash(String hash);

    long insertPatient(NewPatient patient);

    long insertSchedule(NewSchedule schedule);

    void insertSlot(NewSlot slot);

    Optional<DoctorSchedule> findSchedule(long id);

    List<DoctorSchedule> listSchedules(LocalDate date, Long deptId, Long doctorId);

    List<ScheduleSlot> listSlots(long scheduleId);

    int closeSchedule(long id, long doctorId);

    int closeSlots(long scheduleId);

    Optional<SlotBookingInfo> findSlotBooking(long slotId);

    boolean existsActiveAppointment(long slotId, long patientId);

    int reserveSlot(long slotId);

    int releaseSlot(long slotId);

    long insertAppointment(NewAppointment appointment);

    long nextScheduleSeq(long scheduleId);

    Optional<AppointmentView> findAppointment(long id);

    List<AppointmentView> listAppointments(LocalDate date, String status, Long deptId, Long doctorId);

    int transitionAppointment(long id, String fromStatus, String toStatus, Instant at, String reason);

    int markNoShow(LocalDate date);

    long insertCharge(NewCharge charge);

    Optional<ChargeView> findCharge(long id);

    Optional<ChargeView> findChargeByBiz(String bizType, long bizId);

    int markChargePaid(long id, String paymentMethod, Instant at, long cashierId);

    int markChargeStatus(long id, String status, Long operatorId);

    long insertRefund(NewRefund refund);

    Optional<Long> findRefundChargeId(long chargeId);

    long insertEncounter(NewEncounter encounter);

    Optional<Long> findEncounterId(long appointmentId);

    void updateEncounterId(long appointmentId, long encounterId);

    long nextSequence(String name);

    // idempotency
    void insertIdempotency(long userId, String key, String method, String path, String requestHash,
            String status, Instant expiresAt);

    Optional<IdemRecord> findIdempotency(long userId, String key);

    void completeIdempotency(long userId, String key, int responseStatus, String responseBody);

    void failIdempotency(long userId, String key, String errorCode);
}
