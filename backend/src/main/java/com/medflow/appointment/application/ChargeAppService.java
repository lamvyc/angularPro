package com.medflow.appointment.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.ChargeView;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.iam.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargeAppService {

    private static final Set<String> METHODS = Set.of("CASH", "WECHAT", "ALIPAY", "CARD");

    private final AppointmentRepository repository;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional
    public ChargeView pay(long chargeId, String paymentMethod) {
        String method = paymentMethod == null ? "CASH" : paymentMethod.toUpperCase();
        if (!METHODS.contains(method)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不支持的支付方式");
        }
        ChargeView charge = repository.findCharge(chargeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "收费单不存在"));
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.canAccessDepartment(charge.deptId())) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权收取其他科室费用");
        }
        if ("PAID".equals(charge.status())) {
            return charge;
        }
        if (!"UNPAID".equals(charge.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前账单状态不允许收费");
        }
        Instant now = Instant.now(clock);
        if (repository.markChargePaid(chargeId, method, now, user.id()) == 0) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT);
        }
        auditService.success("CHARGE_PAID", "CHARGE", String.valueOf(chargeId),
                Map.of("status", charge.status(), "amount", charge.amount()),
                Map.of("status", "PAID", "paymentMethod", method));
        return repository.findCharge(chargeId).orElseThrow();
    }
}
