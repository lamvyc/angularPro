package com.medflow.appointment.api;

import java.time.LocalDate;
import java.util.List;

import com.medflow.appointment.api.dto.BookAppointmentRequest;
import com.medflow.appointment.api.dto.CancelAppointmentRequest;
import com.medflow.appointment.application.AppointmentAppService;
import com.medflow.appointment.domain.AppointmentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointment", description = "预约、签到与退号")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentAppService appointmentAppService;

    @PostMapping
    @PreAuthorize("hasAuthority('appointment:create')")
    @Operation(summary = "预约挂号（Idempotency-Key 必填）")
    public ResponseEntity<AppointmentView> book(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookAppointmentRequest request) {
        var result = appointmentAppService.book(request.patientId(), request.slotId(), idempotencyKey);
        return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result.value());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    @Operation(summary = "预约列表（受科室数据范围约束）")
    public List<AppointmentView> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long doctorId) {
        return appointmentAppService.list(date, status, deptId, doctorId);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('appointment:cancel')")
    @Operation(summary = "取消预约/退号（已支付自动退款）")
    public AppointmentView cancel(@PathVariable long id,
            @RequestBody(required = false) CancelAppointmentRequest request) {
        return appointmentAppService.cancel(id, request == null ? null : request.reason());
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('appointment:checkin')")
    @Operation(summary = "签到并创建候诊 encounter")
    public AppointmentView checkIn(@PathVariable long id) {
        return appointmentAppService.checkIn(id);
    }
}
