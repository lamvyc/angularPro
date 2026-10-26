package com.medflow.prescription.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.medflow.appointment.application.BusinessNumberService;
import com.medflow.common.error.BusinessException;
import com.medflow.common.error.ErrorCode;
import com.medflow.common.security.CurrentUser;
import com.medflow.common.security.SecurityUtils;
import com.medflow.encounter.domain.EncounterRepository;
import com.medflow.encounter.domain.EncounterRow;
import com.medflow.iam.application.AuditService;
import com.medflow.prescription.domain.PrescriptionModels;
import com.medflow.prescription.domain.PrescriptionModels.Drug;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.Item;
import com.medflow.prescription.domain.PrescriptionModels.ItemCommand;
import com.medflow.prescription.domain.PrescriptionModels.Reservation;
import com.medflow.prescription.domain.PrescriptionModels.Status;
import com.medflow.prescription.domain.PrescriptionModels.View;
import com.medflow.prescription.domain.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class PrescriptionTxService {

    private final PrescriptionRepository repository;
    private final EncounterRepository encounterRepository;
    private final BusinessNumberService numbers;
    private final AuditService auditService;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional
    public View create(long encounterId, PrescriptionModels.CreateCommand command) {
        EncounterRow encounter = requireEncounter(encounterId);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        if (encounter.doctorId() != user.id() || !"IN_PROGRESS".equals(encounter.status())) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只有接诊医生可以在接诊中开方");
        }
        List<Item> items = buildItems(command.items());
        long id = repository.insertPrescription(numbers.next("RX"), encounterId, encounter.patientId(),
                encounter.deptId(), encounter.doctorId(), normalizeType(command.type()),
                trimToNull(command.diagnosisSummary()), trimToNull(command.medicalAdvice()), user.id());
        repository.replaceItems(id, items);
        auditService.success("PRESCRIPTION_CREATED", "PRESCRIPTION", String.valueOf(id), null,
                mapOf("encounterId", encounterId, "items", items.size()));
        return view(id);
    }

    @Transactional
    public View update(long id, PrescriptionModels.CreateCommand command) {
        Header header = requireHeader(id);
        requireOwnDoctor(header);
        if (!"DRAFT".equals(header.status()) && !"REJECTED".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有草稿或驳回处方可以修改");
        }
        List<Item> items = buildItems(command.items());
        if (repository.updateHeader(id, normalizeType(command.type()), trimToNull(command.diagnosisSummary()),
                trimToNull(command.medicalAdvice()), header.version(), SecurityUtils.requireCurrentUser().id()) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        repository.replaceItems(id, items);
        auditService.success("PRESCRIPTION_UPDATED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status()), mapOf("items", items.size()));
        return view(id);
    }

    @Transactional
    public View submit(long id, int version) {
        Header header = requireHeader(id);
        requireOwnDoctor(header);
        if (!"DRAFT".equals(header.status()) && !"REJECTED".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前处方状态不允许提交");
        }
        List<Item> items = repository.listItems(id);
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "处方至少需要一条明细");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        int revision = header.revisionNo() + 1;
        if (repository.submit(id, header.status(), version, now, user.id()) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        Header submitted = requireHeader(id);
        String content = contentJson(submitted, items);
        repository.insertRevision(id, revision, content, sha256(content), user.id(), now);
        event(id, "SUBMIT", header.status(), "SUBMITTED", user.id(), null,
                mapOf("revisionNo", revision));
        auditService.success("PRESCRIPTION_SUBMITTED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status(), "revisionNo", header.revisionNo()),
                mapOf("status", "SUBMITTED", "revisionNo", revision, "hash", sha256(content)));
        return view(id);
    }

    @Transactional
    public View startReview(long id, int version) {
        Header header = requireHeader(id);
        if (!"SUBMITTED".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有已提交处方可以开始审核");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (repository.startReview(id, version, user.id(), now) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        event(id, "START_REVIEW", header.status(), "UNDER_REVIEW", user.id(), null, Map.of());
        auditService.success("PRESCRIPTION_REVIEW_STARTED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status()), mapOf("status", "UNDER_REVIEW"));
        return view(id);
    }

    @Transactional
    public View approve(long id, int version) {
        Header header = requireHeader(id);
        if (!"UNDER_REVIEW".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有审核中处方可以通过");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (repository.approve(id, version, user.id(), now, now.plus(Duration.ofHours(72))) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        reserveStock(id, header.revisionNo(), user.id());
        repository.insertReview(id, header.revisionNo(), user.id(), "APPROVED", null, now);
        event(id, "APPROVE", header.status(), "APPROVED", user.id(), null, Map.of());
        auditService.success("PRESCRIPTION_APPROVED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status()), mapOf("status", "APPROVED", "expiresInHours", 72));
        return view(id);
    }

    @Transactional
    public View reject(long id, int version, String reason) {
        Header header = requireHeader(id);
        if (!"UNDER_REVIEW".equals(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "只有审核中处方可以驳回");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "驳回原因不能为空");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        Instant now = Instant.now(clock);
        if (repository.reject(id, version, user.id(), reason.trim(), now) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        repository.insertReview(id, header.revisionNo(), user.id(), "REJECTED", reason.trim(), now);
        event(id, "REJECT", header.status(), "REJECTED", user.id(), reason.trim(), Map.of());
        auditService.success("PRESCRIPTION_REJECTED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status()), mapOf("status", "REJECTED", "reason", reason.trim()));
        return view(id);
    }

    @Transactional
    public View cancel(long id, int version, String reason) {
        Header header = requireHeader(id);
        if (!List.of("DRAFT", "SUBMITTED", "UNDER_REVIEW", "APPROVED").contains(header.status())) {
            throw new BusinessException(ErrorCode.STATE_CONFLICT, "当前处方状态不允许取消");
        }
        CurrentUser user = SecurityUtils.requireCurrentUser();
        boolean pharmacist = user.roles().contains("PHARMACIST");
        if (!pharmacist && header.doctorId() != user.id()) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只能取消自己开具的处方");
        }
        if ("APPROVED".equals(header.status())) {
            releaseStock(id, user.id());
        }
        if (repository.cancel(id, version, reason == null ? "取消处方" : reason, user.id(), Instant.now(clock)) == 0) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }
        event(id, "CANCEL", header.status(), "CANCELLED", user.id(), reason, Map.of());
        auditService.success("PRESCRIPTION_CANCELLED", "PRESCRIPTION", String.valueOf(id),
                mapOf("status", header.status()), mapOf("status", "CANCELLED", "reason", reason));
        return view(id);
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Shanghai")
    @Transactional
    public void expireDue() {
        Instant now = Instant.now(clock);
        for (Header header : repository.findExpired(java.time.LocalDate.now(clock), now)) {
            if (repository.expire(header.id(), now) == 0) {
                continue;
            }
            releaseStock(header.id(), null);
            event(header.id(), "EXPIRE", "APPROVED", "EXPIRED", 0L, null, Map.of());
            auditService.success("PRESCRIPTION_EXPIRED", "PRESCRIPTION", String.valueOf(header.id()),
                    mapOf("status", "APPROVED"), mapOf("status", "EXPIRED"));
        }
    }

    private void reserveStock(long prescriptionId, int revisionNo, long userId) {
        List<Item> items = repository.listItems(prescriptionId);
        for (Item item : items) {
            int remaining = item.quantity();
            for (PrescriptionRepository.Batch batch : repository.batchesForDrug(item.drugId())) {
                if (remaining <= 0) {
                    break;
                }
                int take = Math.min(remaining, batch.available());
                if (take <= 0 || repository.reserveBatch(batch.id(), take) == 0) {
                    continue;
                }
                String idem = "RX:" + prescriptionId + ":" + revisionNo + ":" + item.id() + ":" + batch.id();
                repository.insertReservation(prescriptionId, item.id(), item.drugId(), batch.id(), take, idem);
                repository.insertMovement(numbers.next("MV"), item.drugId(), batch.id(), "RESERVE", take,
                        batch.available(), batch.available() - take, "PRESCRIPTION", prescriptionId, idem, userId,
                        "审方通过预占");
                remaining -= take;
            }
            if (remaining > 0) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                        "药品 " + item.drugName() + " 库存不足，可用 " + repository.availableStock(item.drugId()));
            }
        }
    }

    private void releaseStock(long prescriptionId, Long userId) {
        for (Reservation reservation : repository.heldReservations(prescriptionId)) {
            if (repository.releaseReservation(reservation.id()) == 0) {
                continue;
            }
            repository.releaseBatch(reservation.batchId(), reservation.quantity());
            repository.insertMovement(numbers.next("MV"), reservation.drugId(), reservation.batchId(),
                    "RELEASE", reservation.quantity(), reservation.quantity(), 0, "PRESCRIPTION",
                    prescriptionId, "REL:" + reservation.id(), userId, "释放预占");
        }
    }

    private List<Item> buildItems(List<ItemCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "处方至少需要一条明细");
        }
        List<Item> items = new ArrayList<>();
        for (ItemCommand command : commands) {
            if (command.dosage() == null || command.dosage().isBlank()
                    || command.frequency() == null || command.frequency().isBlank()
                    || command.route() == null || command.route().isBlank()
                    || command.days() <= 0 || command.quantity() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "剂量、频次、途径、天数、数量均不能为空且必须大于 0");
            }
            Drug drug = repository.findDrug(command.drugId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "药品不存在或已停用"));
            BigDecimal amount = drug.unitPrice().multiply(BigDecimal.valueOf(command.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            items.add(new Item(0, drug.id(), drug.name(), drug.spec(), command.dosage().trim(),
                    command.frequency().trim(), command.route().trim(), command.days(), command.quantity(),
                    drug.unit(), drug.unitPrice(), amount, trimToNull(command.note())));
        }
        return items;
    }

    private View view(long id) {
        Header header = requireHeader(id);
        return new View(header, repository.listItems(id));
    }

    private Header requireHeader(long id) {
        return repository.findHeader(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "处方不存在"));
    }

    private EncounterRow requireEncounter(long id) {
        return encounterRepository.find(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "就诊不存在"));
    }

    private void requireOwnDoctor(Header header) {
        if (header.doctorId() != SecurityUtils.requireCurrentUser().id()) {
            throw new BusinessException(ErrorCode.AUTHZ_DENIED, "只有开具医生可以执行该操作");
        }
    }

    private String normalizeType(String type) {
        return List.of("WESTERN", "HERBAL", "TREATMENT").contains(type) ? type : "WESTERN";
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String contentJson(Header header, List<Item> items) {
        return jsonMapper.writeValueAsString(mapOf(
                "prescriptionNo", header.prescriptionNo(),
                "revisionNo", header.revisionNo(),
                "type", header.type(),
                "diagnosisSummary", header.diagnosisSummary(),
                "medicalAdvice", header.medicalAdvice(),
                "items", items));
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

    private void event(long id, String action, String from, String to, long actorId, String reason,
            Map<String, Object> payload) {
        repository.insertEvent(id, action, from, to, actorId, reason, jsonMapper.writeValueAsString(payload));
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
