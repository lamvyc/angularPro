package com.medflow.pharmacy.api;

import java.util.List;

import com.medflow.appointment.application.IdempotencyService;
import com.medflow.pharmacy.api.dto.BatchRequest;
import com.medflow.pharmacy.api.dto.ReturnLineRequest;
import com.medflow.pharmacy.api.dto.ReturnRequest;
import com.medflow.pharmacy.application.PharmacyAppService;
import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.MovementView;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.ReturnLine;
import com.medflow.pharmacy.domain.PharmacyModels.Summary;
import com.medflow.prescription.domain.PrescriptionModels.View;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1")
@Tag(name = "Pharmacy Stock", description = "批次入库、发药、退药与对账")
@RequiredArgsConstructor
public class StockController {

    private final PharmacyAppService pharmacyAppService;

    @GetMapping("/stock/batches")
    @PreAuthorize("hasAuthority('stock:read')")
    public List<BatchView> batches(@RequestParam(required = false) Long drugId) {
        return pharmacyAppService.batches(drugId);
    }

    @PostMapping("/stock/batches")
    @PreAuthorize("hasAuthority('stock:manage')")
    @Operation(summary = "批次入库（过期批次拒绝）")
    public BatchView createBatch(@Valid @RequestBody BatchRequest request) {
        return pharmacyAppService.createBatch(request.drugId(), request.batchNo(), request.productionDate(),
                request.expiryDate(), request.quantity());
    }

    @GetMapping("/stock/summary")
    @PreAuthorize("hasAuthority('stock:read')")
    public List<Summary> summary() {
        return pharmacyAppService.summary();
    }

    @GetMapping("/stock/movements")
    @PreAuthorize("hasAuthority('stock:read')")
    public List<MovementView> movements(@RequestParam(defaultValue = "100") int limit) {
        return pharmacyAppService.movements(limit);
    }

    @GetMapping("/prescriptions/{id}/reservations")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public List<ReservationView> reservations(@PathVariable long id) {
        return pharmacyAppService.reservations(id);
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    @Operation(summary = "发药（扣减实库存与预占，重复发药幂等）")
    public View dispense(@PathVariable long id) {
        return pharmacyAppService.dispense(id);
    }

    @PostMapping("/prescriptions/{id}/returns")
    @PreAuthorize("hasAuthority('pharmacy:return')")
    @Operation(summary = "退药（按原批次，过期批次转隔离）")
    public View returns(@PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnRequest request) {
        var result = pharmacyAppService.returnStock(id, idempotencyKey, request.reason(),
                request.lines().stream().map(line -> new ReturnLine(line.reservationId(), line.quantity()))
                        .toList());
        return result.value();
    }
}
