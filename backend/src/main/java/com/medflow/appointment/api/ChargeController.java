package com.medflow.appointment.api;

import com.medflow.appointment.api.dto.PayChargeRequest;
import com.medflow.appointment.application.ChargeAppService;
import com.medflow.appointment.domain.ChargeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charges")
@Tag(name = "Charge", description = "挂号收费")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeAppService chargeAppService;

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('charge:collect')")
    @Operation(summary = "收费（重复收费幂等）")
    public ChargeView pay(@PathVariable long id, @Valid @RequestBody PayChargeRequest request) {
        return chargeAppService.pay(id, request.paymentMethod());
    }
}
