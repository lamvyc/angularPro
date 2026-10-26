package com.medflow.appointment.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.DoctorSchedule;
import com.medflow.appointment.domain.NewSchedule;
import com.medflow.appointment.domain.NewSlot;
import com.medflow.appointment.domain.ScheduleSlot;
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
public class ScheduleAppService {

    private final AppointmentRepository repository;
    private final DataScopeResolver dataScopeResolver;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public DoctorSchedule create(CreateScheduleCommand command) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.roles().contains("DOCTOR")) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只有医生可以创建排班");
        }
        if (command.scheduleDate() == null || command.scheduleDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "排班日期不能早于今天");
        }
        if (!"AM".equals(command.period()) && !"PM".equals(command.period())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "时段只能是 AM 或 PM");
        }
        if (command.startTime() == null || command.endTime() == null
                || !command.endTime().isAfter(command.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "开始时间必须早于结束时间");
        }
        if (command.slotCount() < 1 || command.slotCount() > 50) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "号源数量需在 1-50 之间");
        }
        if (command.capacityPerSlot() < 1 || command.capacityPerSlot() > 20) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "每个号源容量需在 1-20 之间");
        }
        BigDecimal fee = command.registrationFee() == null ? new BigDecimal("20.00") : command.registrationFee();
        if (fee.signum() < 0 || fee.compareTo(new BigDecimal("1000")) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "挂号费不合法");
        }
        int minutes = (int) Duration.between(command.startTime(), command.endTime()).toMinutes();
        int slotMinutes = minutes / command.slotCount();
        if (slotMinutes < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "时段太短，无法生成号源");
        }

        long scheduleId;
        try {
            scheduleId = repository.insertSchedule(new NewSchedule(
                    user.deptId(), user.id(), command.scheduleDate(), command.period(),
                    command.startTime(), command.endTime(), trimToNull(command.room()), fee,
                    Instant.now(clock), user.id()));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "该医生当天该时段已有排班");
        }
        for (int i = 0; i < command.slotCount(); i++) {
            LocalTime slotStart = command.startTime().plusMinutes((long) i * slotMinutes);
            LocalTime slotEnd = slotStart.plusMinutes(slotMinutes);
            repository.insertSlot(new NewSlot(scheduleId, i + 1, slotStart, slotEnd,
                    command.capacityPerSlot()));
        }
        DoctorSchedule schedule = require(scheduleId);
        auditService.success("SCHEDULE_CREATED", "SCHEDULE", String.valueOf(scheduleId), null,
                mapOf("date", command.scheduleDate(), "period", command.period(),
                        "slotCount", command.slotCount(), "capacity", command.capacityPerSlot()));
        return schedule;
    }

    public List<DoctorSchedule> list(LocalDate date, Long deptId, Long doctorId) {
        Long scopedDept = dataScopeResolver.resolveDepartmentFilter(deptId).orElse(null);
        return repository.listSchedules(date, scopedDept, doctorId);
    }

    public List<ScheduleSlot> slots(long scheduleId) {
        DoctorSchedule schedule = require(scheduleId);
        dataScopeResolver.resolveDepartmentFilter(schedule.deptId());
        return repository.listSlots(scheduleId);
    }

    @Transactional
    public DoctorSchedule close(long scheduleId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        DoctorSchedule schedule = require(scheduleId);
        if (schedule.doctorId() != user.id()) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只能关闭自己的排班");
        }
        repository.closeSchedule(scheduleId, user.id());
        repository.closeSlots(scheduleId);
        auditService.success("SCHEDULE_CLOSED", "SCHEDULE", String.valueOf(scheduleId),
                mapOf("status", schedule.status()), mapOf("status", "CLOSED"));
        return require(scheduleId);
    }

    private DoctorSchedule require(long id) {
        return repository.findSchedule(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "排班不存在"));
    }

    private String trimToNull(String value) {
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
