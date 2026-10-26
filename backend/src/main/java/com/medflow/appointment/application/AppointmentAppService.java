package com.medflow.appointment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.AppointmentView;
import com.medflow.appointment.domain.ChargeView;
import com.medflow.appointment.domain.NewEncounter;
import com.medflow.appointment.domain.NewRefund;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.DataScope;
import com.medflow.common.security.DataScopeResolver;
import com.medflow.common.security.SecurityUtils;
import com.medflow.iam.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentAppService {

    private final AppointmentRepository repository;
    private final AppointmentBookingService bookingService;
    private final IdempotencyService idempotencyService;
    private final DataScopeResolver dataScopeResolver;
    private final BusinessNumberService numbers;
    private final AuditService auditService;
    private final Clock clock;

    public IdempotencyService.IdempotentResult<AppointmentView> book(long patientId, long slotId,
            String idempotencyKey) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return idempotencyService.execute(user.id(), idempotencyKey, "POST", "/api/v1/appointments",
                Map.of("patientId", patientId, "slotId", slotId), AppointmentView.class, 201,
                () -> bookingService.book(patientId, slotId, "WINDOW"));
    }

    public List<AppointmentView> list(LocalDate date, String status, Long deptId, Long doctorId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Long scopedDept = dataScopeResolver.resolveDepartmentFilter(deptId).orElse(null);
        Long scopedDoctor = user.dataScope() == DataScope.SELF ? Long.valueOf(user.id()) : doctorId;
        return repository.listAppointments(date, blankToNull(status), scopedDept, scopedDoctor);
    }

    @Transactional
    public AppointmentView cancel(long appointmentId, String reason) {
        AppointmentView appointment = requireAccessible(appointmentId);
        if ("CANCELLED".equals(appointment.status())) {
            return appointment;
        }
        if (!"BOOKED".equals(appointment.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前预约状态不允许取消");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (repository.transitionAppointment(appointmentId, "BOOKED", "CANCELLED", now, reason) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        repository.releaseSlot(appointment.slotId());

        ChargeView charge = repository.findChargeByBiz("REGISTRATION", appointmentId).orElse(null);
        if (charge != null && "PAID".equals(charge.status())) {
            if (repository.findRefundChargeId(charge.id()).isEmpty()) {
                repository.insertRefund(new NewRefund(
                        numbers.next("RF"), charge.id(), "REGISTRATION", appointmentId, charge.amount(),
                        reason == null ? "患者取消预约" : reason, "SUCCESS", user.id(), now,
                        "refund:REGISTRATION:" + appointmentId));
            }
            repository.markChargeStatus(charge.id(), "REFUNDED", user.id());
        } else if (charge != null && "UNPAID".equals(charge.status())) {
            repository.markChargeStatus(charge.id(), "CANCELLED", user.id());
        }
        auditService.success("APPOINTMENT_CANCELLED", "APPOINTMENT", String.valueOf(appointmentId),
                Map.of("status", appointment.status()),
                mapOf("status", "CANCELLED", "reason", reason == null ? "患者取消预约" : reason));
        return repository.findAppointment(appointmentId).orElseThrow();
    }

    @Transactional
    public AppointmentView checkIn(long appointmentId) {
        AppointmentView appointment = requireAccessible(appointmentId);
        if (!"BOOKED".equals(appointment.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已预约状态可以签到");
        }
        if (!LocalDate.now(clock).equals(appointment.appointmentDate())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只能在就诊当日签到");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (repository.transitionAppointment(appointmentId, "BOOKED", "CHECKED_IN", now, null) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        Long encounterId = repository.findEncounterId(appointmentId).orElse(null);
        if (encounterId == null) {
            encounterId = repository.insertEncounter(new NewEncounter(
                    numbers.next("EN"), appointmentId, appointment.patientId(), appointment.deptId(),
                    appointment.doctorId(), "WAITING", user.id()));
            repository.updateEncounterId(appointmentId, encounterId);
        }
        auditService.success("APPOINTMENT_CHECKED_IN", "APPOINTMENT", String.valueOf(appointmentId),
                Map.of("status", "BOOKED"), mapOf("status", "CHECKED_IN", "encounterId", encounterId));
        return repository.findAppointment(appointmentId).orElseThrow();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    @Transactional
    public void markNoShow() {
        LocalDate today = LocalDate.now(clock);
        int count = repository.markNoShow(today);
        if (count > 0) {
            auditService.success("APPOINTMENT_NO_SHOW_JOB", "APPOINTMENT", today.toString(), null,
                    Map.of("count", count));
        }
    }

    private AppointmentView requireAccessible(long appointmentId) {
        AppointmentView appointment = repository.findAppointment(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "预约不存在"));
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.canAccessDepartment(appointment.deptId())) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权访问其他科室的预约");
        }
        return appointment;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
