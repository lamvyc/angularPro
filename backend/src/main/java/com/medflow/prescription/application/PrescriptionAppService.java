package com.medflow.prescription.application;

import java.util.List;
import java.util.Map;

import com.medflow.appointment.application.IdempotencyService;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.prescription.domain.PrescriptionModels;
import com.medflow.prescription.domain.PrescriptionModels.Drug;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.View;
import com.medflow.prescription.domain.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrescriptionAppService {

    private final PrescriptionTxService txService;
    private final PrescriptionRepository repository;
    private final IdempotencyService idempotencyService;

    public List<Drug> drugs() {
        return repository.listDrugs();
    }

    public View create(long encounterId, PrescriptionModels.CreateCommand command) {
        return txService.create(encounterId, command);
    }

    public View update(long id, PrescriptionModels.CreateCommand command) {
        return txService.update(id, command);
    }

    public IdempotencyService.IdempotentResult<View> submit(long id, int version, String idempotencyKey) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return idempotencyService.execute(user.id(), idempotencyKey, "POST",
                "/api/v1/prescriptions/" + id + "/submit", Map.of("version", version), View.class, 200,
                () -> txService.submit(id, version));
    }

    public View startReview(long id, int version) {
        return txService.startReview(id, version);
    }

    public View approve(long id, int version) {
        return txService.approve(id, version);
    }

    public View reject(long id, int version, String reason) {
        return txService.reject(id, version, reason);
    }

    public View cancel(long id, int version, String reason) {
        return txService.cancel(id, version, reason);
    }

    public List<Header> list(Long encounterId, String status) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Long doctorId = user.roles().contains("DOCTOR") ? Long.valueOf(user.id()) : null;
        if (!user.roles().contains("DOCTOR") && !user.roles().contains("PHARMACIST")) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权查看处方");
        }
        return repository.listHeaders(encounterId, status == null || status.isBlank() ? null : status, doctorId);
    }

    public View detail(long id) {
        Header header = repository.findHeader(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "处方不存在"));
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (!user.roles().contains("PHARMACIST") && header.doctorId() != user.id()) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "无权查看其他医生的处方");
        }
        return new View(header, repository.listItems(id));
    }
}
