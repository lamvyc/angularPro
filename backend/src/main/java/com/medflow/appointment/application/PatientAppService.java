package com.medflow.appointment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.medflow.appointment.domain.AppointmentRepository;
import com.medflow.appointment.domain.NewPatient;
import com.medflow.appointment.domain.Patient;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.common.web.PageResult;
import com.medflow.iam.application.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientAppService {

    private final AppointmentRepository repository;
    private final BusinessNumberService numbers;
    private final AuditService auditService;

    @Value("${medflow.patient.id-card-salt:medflow-demo-salt}")
    private String idCardSalt;

    public PageResult<Patient> search(int page, int size, String keyword) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        int offset = (safePage - 1) * safeSize;
        return PageResult.of(repository.searchPatients(normalized, offset, safeSize),
                safePage, safeSize, repository.countPatients(normalized));
    }

    public Patient get(long id) {
        return repository.findPatient(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "患者不存在"));
    }

    @Transactional
    public Patient create(CreatePatientCommand command) {
        String name = requireText(command.name(), "患者姓名不能为空", 50);
        if (command.birthDate() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "出生日期不能为空");
        }
        String idCardNo = command.idCardNo() == null ? null : command.idCardNo().trim();
        String hash = idCardNo == null || idCardNo.isBlank() ? null : sha256(idCardSalt + idCardNo);
        if (hash != null && repository.existsIdCardHash(hash)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该证件号已建档");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        String patientNo = numbers.next("P");
        Patient patient = repository.findPatient(repository.insertPatient(new NewPatient(
                patientNo,
                name,
                normalizeGender(command.gender()),
                command.birthDate(),
                hash,
                maskIdCard(idCardNo),
                trimToNull(command.phone()),
                trimToNull(command.address()),
                trimToNull(command.allergyHistory()),
                user.id()))).orElseThrow();
        auditService.success("PATIENT_CREATED", "PATIENT", String.valueOf(patient.id()), null,
                mapOf("patientNo", patient.patientNo(), "name", patient.name(), "phone", patient.phone()));
        return patient;
    }

    private String requireText(String value, String message, int max) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "字段长度超出限制");
        }
        return trimmed;
    }

    private String normalizeGender(String gender) {
        return switch (gender == null ? "UNKNOWN" : gender.toUpperCase()) {
            case "MALE", "FEMALE" -> gender.toUpperCase();
            default -> "UNKNOWN";
        };
    }

    private String maskIdCard(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 10) {
            return null;
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
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
