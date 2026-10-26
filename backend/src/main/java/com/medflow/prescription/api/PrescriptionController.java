package com.medflow.prescription.api;

import java.util.List;

import com.medflow.appointment.application.IdempotencyService;
import com.medflow.prescription.api.dto.CancelPrescriptionRequest;
import com.medflow.prescription.api.dto.CreatePrescriptionRequest;
import com.medflow.prescription.api.dto.PrescriptionVersionRequest;
import com.medflow.prescription.api.dto.RejectPrescriptionRequest;
import com.medflow.prescription.application.PrescriptionAppService;
import com.medflow.prescription.domain.PrescriptionModels;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.ItemCommand;
import com.medflow.prescription.domain.PrescriptionModels.View;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescription", description = "处方、审方与库存预占")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionAppService prescriptionAppService;

    @PostMapping("/encounters/{encounterId}")
    @PreAuthorize("hasAuthority('prescription:create')")
    public View create(@PathVariable long encounterId, @Valid @RequestBody CreatePrescriptionRequest request) {
        return prescriptionAppService.create(encounterId, command(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('prescription:create')")
    public View update(@PathVariable long id, @Valid @RequestBody CreatePrescriptionRequest request) {
        return prescriptionAppService.update(id, command(request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('prescription:submit')")
    @Operation(summary = "提交处方（Idempotency-Key 必填，保存修订快照）")
    public ResponseEntity<View> submit(@PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PrescriptionVersionRequest request) {
        var result = prescriptionAppService.submit(id, request.version(), idempotencyKey);
        return ResponseEntity.ok(result.value());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('prescription:read')")
    public List<Header> list(@RequestParam(required = false) Long encounterId,
            @RequestParam(required = false) String status) {
        return prescriptionAppService.list(encounterId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('prescription:read')")
    public View detail(@PathVariable long id) {
        return prescriptionAppService.detail(id);
    }

    @PostMapping("/{id}/review/start")
    @PreAuthorize("hasAuthority('pharmacy:review')")
    public View startReview(@PathVariable long id, @Valid @RequestBody PrescriptionVersionRequest request) {
        return prescriptionAppService.startReview(id, request.version());
    }

    @PostMapping("/{id}/review/approve")
    @PreAuthorize("hasAuthority('pharmacy:review')")
    @Operation(summary = "审方通过并按 FEFO 预占库存")
    public View approve(@PathVariable long id, @Valid @RequestBody PrescriptionVersionRequest request) {
        return prescriptionAppService.approve(id, request.version());
    }

    @PostMapping("/{id}/review/reject")
    @PreAuthorize("hasAuthority('pharmacy:review')")
    public View reject(@PathVariable long id, @Valid @RequestBody RejectPrescriptionRequest request) {
        return prescriptionAppService.reject(id, request.version(), request.reason());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('prescription:cancel', 'pharmacy:review')")
    public View cancel(@PathVariable long id, @Valid @RequestBody CancelPrescriptionRequest request) {
        return prescriptionAppService.cancel(id, request.version(), request.reason());
    }

    private PrescriptionModels.CreateCommand command(CreatePrescriptionRequest request) {
        return new PrescriptionModels.CreateCommand(request.type(), request.diagnosisSummary(),
                request.medicalAdvice(), request.items().stream()
                        .map(item -> new ItemCommand(item.drugId(), item.dosage(), item.frequency(),
                                item.route(), item.days(), item.quantity(), item.note()))
                        .toList());
    }
}
