package com.medflow.appointment.api;

import java.time.LocalDate;
import java.util.List;

import com.medflow.appointment.api.dto.CreateScheduleRequest;
import com.medflow.appointment.application.CreateScheduleCommand;
import com.medflow.appointment.application.ScheduleAppService;
import com.medflow.appointment.domain.DoctorSchedule;
import com.medflow.appointment.domain.ScheduleSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedule", description = "医生排班与号源")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleAppService scheduleAppService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('schedule:manage')")
    @Operation(summary = "创建排班并生成号源")
    public DoctorSchedule create(@Valid @RequestBody CreateScheduleRequest request) {
        return scheduleAppService.create(new CreateScheduleCommand(
                request.scheduleDate(), request.period(), request.startTime(), request.endTime(),
                request.room(), request.slotCount(), request.capacityPerSlot(), request.registrationFee()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('schedule:read')")
    @Operation(summary = "查询排班（受数据范围约束）")
    public List<DoctorSchedule> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long doctorId) {
        return scheduleAppService.list(date, deptId, doctorId);
    }

    @GetMapping("/{id}/slots")
    @PreAuthorize("hasAuthority('schedule:read')")
    public List<ScheduleSlot> slots(@PathVariable long id) {
        return scheduleAppService.slots(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('schedule:manage')")
    @Operation(summary = "关闭自己的排班")
    public DoctorSchedule close(@PathVariable long id) {
        return scheduleAppService.close(id);
    }
}
