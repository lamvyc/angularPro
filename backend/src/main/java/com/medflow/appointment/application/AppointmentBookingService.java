package com.medflow.appointment.application;

import java.time.Clock;
import java.time.LocalDate;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.AppointmentView;
import com.medflow.appointment.domain.NewAppointment;
import com.medflow.appointment.domain.NewCharge;
import com.medflow.appointment.domain.Patient;
import com.medflow.appointment.domain.SlotBookingInfo;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.DataScopeResolver;
import com.medflow.common.security.SecurityUtils;
import com.medflow.iam.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    private final AppointmentRepository repository;
    private final DataScopeResolver dataScopeResolver;
    private final BusinessNumberService numbers;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public AppointmentView book(long patientId, long slotId, String source) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        SlotBookingInfo slot = repository.findSlotBooking(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "号源不存在"));
        dataScopeResolver.resolveDepartmentFilter(slot.deptId());
        if (!"PUBLISHED".equals(slot.scheduleStatus()) || !"OPEN".equals(slot.slotStatus())) {
            throw new BusinessException(ErrorCode.SLOT_FULL, "号源已关闭或排班未发布");
        }
        if (slot.scheduleDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.EXPIRED, "号源已过期");
        }
        Patient patient = repository.findPatient(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "患者不存在"));
        if (!"ACTIVE".equals(patient.status())) {
            throw new BusinessException(ErrorCode.CONFLICT, "患者档案已归档");
        }
        if (repository.existsActiveAppointment(slotId, patientId)) {
            throw new BusinessException(ErrorCode.SLOT_DUPLICATE);
        }
        if (slot.bookedCount() >= slot.capacity()) {
            throw new BusinessException(ErrorCode.SLOT_FULL);
        }
        if (repository.reserveSlot(slotId) == 0) {
            throw new BusinessException(ErrorCode.SLOT_FULL);
        }

        long appointmentId;
        try {
            appointmentId = repository.insertAppointment(new NewAppointment(
                    numbers.next("AP"), patientId, slot.deptId(), slot.doctorId(), slot.scheduleId(),
                    slotId, slot.scheduleDate(), slot.period(), (int) repository.nextScheduleSeq(slot.scheduleId()), "BOOKED",
                    source == null ? "WINDOW" : source, user.id()));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SLOT_DUPLICATE);
        }
        long chargeId = repository.insertCharge(new NewCharge(
                numbers.next("CH"), "REGISTRATION", appointmentId, patientId, slot.deptId(),
                slot.registrationFee(), "UNPAID", user.id()));
        auditService.success("APPOINTMENT_BOOKED", "APPOINTMENT", String.valueOf(appointmentId), null,
                java.util.Map.of("patientId", patientId, "slotId", slotId, "chargeId", chargeId));
        return repository.findAppointment(appointmentId).orElseThrow();
    }
}
