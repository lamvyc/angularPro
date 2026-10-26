package com.medflow.appointment.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.appointment.domain.AppointmentRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentMapper mapper;

    @Override
    public Optional<Patient> findPatient(long id) {
        return mapper.findPatient(id);
    }

    @Override
    public Optional<Patient> findPatientByNo(String patientNo) {
        return mapper.findPatientByNo(patientNo);
    }

    @Override
    public List<Patient> searchPatients(String keyword, int offset, int size) {
        return mapper.searchPatients(keyword, offset, size);
    }

    @Override
    public long countPatients(String keyword) {
        return mapper.countPatients(keyword);
    }

    @Override
    public boolean existsIdCardHash(String hash) {
        return hash != null && mapper.countIdCardHash(hash) > 0;
    }

    @Override
    public long insertPatient(NewPatient patient) {
        mapper.insertPatient(patient);
        return mapper.lastInsertId();
    }

    @Override
    public long insertSchedule(NewSchedule schedule) {
        mapper.insertSchedule(schedule);
        return mapper.lastInsertId();
    }

    @Override
    public void insertSlot(NewSlot slot) {
        mapper.insertSlot(slot);
    }

    @Override
    public Optional<DoctorSchedule> findSchedule(long id) {
        return mapper.findSchedule(id);
    }

    @Override
    public List<DoctorSchedule> listSchedules(LocalDate date, Long deptId, Long doctorId) {
        return mapper.listSchedules(date, deptId, doctorId);
    }

    @Override
    public List<ScheduleSlot> listSlots(long scheduleId) {
        return mapper.listSlots(scheduleId);
    }

    @Override
    public int closeSchedule(long id, long doctorId) {
        return mapper.closeSchedule(id, doctorId);
    }

    @Override
    public int closeSlots(long scheduleId) {
        return mapper.closeSlots(scheduleId);
    }

    @Override
    public Optional<SlotBookingInfo> findSlotBooking(long slotId) {
        return mapper.findSlotBooking(slotId);
    }

    @Override
    public boolean existsActiveAppointment(long slotId, long patientId) {
        return mapper.countActiveAppointment(slotId, patientId) > 0;
    }

    @Override
    public int reserveSlot(long slotId) {
        return mapper.reserveSlot(slotId);
    }

    @Override
    public int releaseSlot(long slotId) {
        return mapper.releaseSlot(slotId);
    }

    @Override
    public long insertAppointment(NewAppointment appointment) {
        mapper.insertAppointment(appointment);
        return mapper.lastInsertId();
    }

    @Override
    public Optional<AppointmentView> findAppointment(long id) {
        return mapper.findAppointment(id);
    }

    @Override
    public List<AppointmentView> listAppointments(LocalDate date, String status, Long deptId, Long doctorId) {
        return mapper.listAppointments(date, status, deptId, doctorId);
    }

    @Override
    public int transitionAppointment(long id, String fromStatus, String toStatus, Instant at, String reason) {
        return mapper.transitionAppointment(id, fromStatus, toStatus, at, reason);
    }

    @Override
    public int markNoShow(LocalDate date) {
        return mapper.markNoShow(date);
    }

    @Override
    public long insertCharge(NewCharge charge) {
        mapper.insertCharge(charge);
        return mapper.lastInsertId();
    }

    @Override
    public Optional<ChargeView> findCharge(long id) {
        return mapper.findCharge(id);
    }

    @Override
    public Optional<ChargeView> findChargeByBiz(String bizType, long bizId) {
        return mapper.findChargeByBiz(bizType, bizId);
    }

    @Override
    public int markChargePaid(long id, String paymentMethod, Instant at, long cashierId) {
        return mapper.markChargePaid(id, paymentMethod, at, cashierId);
    }

    @Override
    public int markChargeStatus(long id, String status, Long operatorId) {
        return mapper.markChargeStatus(id, status, operatorId);
    }

    @Override
    public long insertRefund(NewRefund refund) {
        mapper.insertRefund(refund);
        return mapper.lastInsertId();
    }

    @Override
    public Optional<Long> findRefundChargeId(long chargeId) {
        return mapper.findRefundChargeId(chargeId);
    }

    @Override
    public long insertEncounter(NewEncounter encounter) {
        mapper.insertEncounter(encounter);
        return mapper.lastInsertId();
    }

    @Override
    public Optional<Long> findEncounterId(long appointmentId) {
        return mapper.findEncounterId(appointmentId);
    }

    @Override
    public void updateEncounterId(long appointmentId, long encounterId) {
        mapper.updateEncounterId(appointmentId, encounterId);
    }

    @Override
    public long nextSequence(String name) {
        int rows = mapper.bumpSequence(name);
        if (rows == 1) {
            return 1L;
        }
        return mapper.lastInsertId();
    }

    @Override
    public void insertIdempotency(long userId, String key, String method, String path, String requestHash,
            String status, Instant expiresAt) {
        mapper.insertIdempotency(userId, key, method, path, requestHash, status, expiresAt);
    }

    @Override
    public Optional<IdemRecord> findIdempotency(long userId, String key) {
        return mapper.findIdempotency(userId, key);
    }

    @Override
    public void completeIdempotency(long userId, String key, int responseStatus, String responseBody) {
        mapper.completeIdempotency(userId, key, responseStatus, responseBody);
    }

    @Override
    public void failIdempotency(long userId, String key, String errorCode) {
        mapper.failIdempotency(userId, key, errorCode);
    }
}
