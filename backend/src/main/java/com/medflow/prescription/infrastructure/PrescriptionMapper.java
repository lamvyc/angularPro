package com.medflow.prescription.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.prescription.domain.PrescriptionModels.Drug;
import com.medflow.prescription.domain.PrescriptionModels.Header;
import com.medflow.prescription.domain.PrescriptionModels.Item;
import com.medflow.prescription.domain.PrescriptionModels.Reservation;
import com.medflow.prescription.domain.PrescriptionRepository.Batch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PrescriptionMapper {

    @Select("""
            SELECT id, drug_code AS code, name, spec, unit, unit_price AS unitPrice,
                   prescription_type AS prescriptionType
            FROM drug WHERE status = 'ENABLED' ORDER BY name
            """)
    List<Drug> listDrugs();

    @Select("""
            SELECT id, drug_code AS code, name, spec, unit, unit_price AS unitPrice,
                   prescription_type AS prescriptionType
            FROM drug WHERE id = #{id} AND status = 'ENABLED'
            """)
    Optional<Drug> findDrug(long id);

    @Insert("""
            INSERT INTO prescription (prescription_no, encounter_id, patient_id, dept_id, doctor_id, type,
                                      status, diagnosis_summary, medical_advice, created_by)
            VALUES (#{no}, #{encounterId}, #{patientId}, #{deptId}, #{doctorId}, #{type}, 'DRAFT',
                    #{diagnosisSummary}, #{medicalAdvice}, #{operatorId})
            """)
    int insertPrescription(@Param("no") String no, @Param("encounterId") long encounterId,
            @Param("patientId") long patientId, @Param("deptId") long deptId, @Param("doctorId") long doctorId,
            @Param("type") String type, @Param("diagnosisSummary") String diagnosisSummary,
            @Param("medicalAdvice") String medicalAdvice, @Param("operatorId") long operatorId);

    @Update("""
            UPDATE prescription SET type = #{type}, diagnosis_summary = #{diagnosisSummary},
                medical_advice = #{medicalAdvice}, updated_by = #{operatorId}, updated_at = NOW(3),
                version = version + 1
            WHERE id = #{id} AND version = #{version} AND status IN ('DRAFT', 'REJECTED')
            """)
    int updateHeader(@Param("id") long id, @Param("type") String type,
            @Param("diagnosisSummary") String diagnosisSummary, @Param("medicalAdvice") String medicalAdvice,
            @Param("version") int version, @Param("operatorId") long operatorId);

    @Insert("""
            INSERT INTO prescription_item (prescription_id, drug_id, drug_name, spec, dosage, frequency, route,
                                           days, quantity, unit, unit_price, amount, note, sort_no)
            VALUES (#{prescriptionId}, #{item.drugId}, #{item.drugName}, #{item.spec}, #{item.dosage},
                    #{item.frequency}, #{item.route}, #{item.days}, #{item.quantity}, #{item.unit},
                    #{item.unitPrice}, #{item.amount}, #{item.note}, #{sortNo})
            """)
    int insertItem(@Param("prescriptionId") long prescriptionId, @Param("item") Item item,
            @Param("sortNo") int sortNo);

    @org.apache.ibatis.annotations.Delete("DELETE FROM prescription_item WHERE prescription_id = #{prescriptionId}")
    int deleteItems(long prescriptionId);

    @Select("""
            SELECT p.id, p.prescription_no AS prescriptionNo, p.encounter_id AS encounterId,
                   p.patient_id AS patientId, pt.name AS patientName, p.dept_id AS deptId, d.name AS deptName,
                   p.doctor_id AS doctorId, u.real_name AS doctorName, p.type, p.status,
                   p.revision_no AS revisionNo, p.diagnosis_summary AS diagnosisSummary,
                   p.medical_advice AS medicalAdvice, p.submitted_at AS submittedAt,
                   p.reviewed_by AS reviewedBy, r.real_name AS reviewerName, p.reviewed_at AS reviewedAt,
                   p.reject_reason AS rejectReason, p.expires_at AS expiresAt, p.created_at AS createdAt,
                   p.version
            FROM prescription p
            JOIN patient pt ON pt.id = p.patient_id
            JOIN sys_department d ON d.id = p.dept_id
            JOIN sys_user u ON u.id = p.doctor_id
            LEFT JOIN sys_user r ON r.id = p.reviewed_by
            WHERE p.id = #{id}
            """)
    Optional<Header> findHeader(long id);

    @Select("""
            <script>
            SELECT p.id, p.prescription_no AS prescriptionNo, p.encounter_id AS encounterId,
                   p.patient_id AS patientId, pt.name AS patientName, p.dept_id AS deptId, d.name AS deptName,
                   p.doctor_id AS doctorId, u.real_name AS doctorName, p.type, p.status,
                   p.revision_no AS revisionNo, p.diagnosis_summary AS diagnosisSummary,
                   p.medical_advice AS medicalAdvice, p.submitted_at AS submittedAt,
                   p.reviewed_by AS reviewedBy, r.real_name AS reviewerName, p.reviewed_at AS reviewedAt,
                   p.reject_reason AS rejectReason, p.expires_at AS expiresAt, p.created_at AS createdAt,
                   p.version
            FROM prescription p
            JOIN patient pt ON pt.id = p.patient_id
            JOIN sys_department d ON d.id = p.dept_id
            JOIN sys_user u ON u.id = p.doctor_id
            LEFT JOIN sys_user r ON r.id = p.reviewed_by
            <where>
              <if test="encounterId != null">AND p.encounter_id = #{encounterId}</if>
              <if test="status != null and status != ''">AND p.status = #{status}</if>
              <if test="doctorId != null">AND p.doctor_id = #{doctorId}</if>
            </where>
            ORDER BY p.created_at DESC, p.id DESC
            </script>
            """)
    List<Header> listHeaders(@Param("encounterId") Long encounterId, @Param("status") String status,
            @Param("doctorId") Long doctorId);

    @Select("""
            SELECT id, drug_id AS drugId, drug_name AS drugName, spec, dosage, frequency, route, days,
                   quantity, unit, unit_price AS unitPrice, amount, note
            FROM prescription_item WHERE prescription_id = #{prescriptionId} ORDER BY sort_no, id
            """)
    List<Item> listItems(long prescriptionId);

    @Update("""
            UPDATE prescription SET status = 'SUBMITTED', revision_no = revision_no + 1,
                submitted_at = #{at}, reject_reason = NULL, reviewed_by = NULL, reviewed_at = NULL,
                updated_by = #{operatorId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND version = #{version} AND status IN ('DRAFT', 'REJECTED')
            """)
    int submit(@Param("id") long id, @Param("fromStatus") String fromStatus, @Param("version") int version,
            @Param("at") Instant at, @Param("operatorId") long operatorId);

    @Update("""
            UPDATE prescription SET status = 'UNDER_REVIEW', reviewed_by = #{reviewerId}, reviewed_at = #{at},
                updated_by = #{reviewerId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND version = #{version} AND status = 'SUBMITTED'
            """)
    int startReview(@Param("id") long id, @Param("version") int version, @Param("reviewerId") long reviewerId,
            @Param("at") Instant at);

    @Update("""
            UPDATE prescription SET status = 'APPROVED', reviewed_by = #{reviewerId}, reviewed_at = #{at},
                expires_at = #{expiresAt}, updated_by = #{reviewerId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND version = #{version} AND status = 'UNDER_REVIEW' AND reviewed_by = #{reviewerId}
            """)
    int approve(@Param("id") long id, @Param("version") int version, @Param("reviewerId") long reviewerId,
            @Param("at") Instant at, @Param("expiresAt") Instant expiresAt);

    @Update("""
            UPDATE prescription SET status = 'REJECTED', reviewed_by = #{reviewerId}, reviewed_at = #{at},
                reject_reason = #{reason}, updated_by = #{reviewerId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND version = #{version} AND status = 'UNDER_REVIEW' AND reviewed_by = #{reviewerId}
            """)
    int reject(@Param("id") long id, @Param("version") int version, @Param("reviewerId") long reviewerId,
            @Param("reason") String reason, @Param("at") Instant at);

    @Update("""
            UPDATE prescription SET status = 'CANCELLED', cancel_reason = #{reason}, updated_by = #{operatorId},
                updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND version = #{version}
              AND status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED')
            """)
    int cancel(@Param("id") long id, @Param("version") int version, @Param("reason") String reason,
            @Param("operatorId") long operatorId, @Param("at") Instant at);

    @Update("""
            UPDATE prescription SET status = 'EXPIRED', updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND status = 'APPROVED'
            """)
    int expire(@Param("id") long id, @Param("at") Instant at);

    @Select("""
            SELECT p.id, p.prescription_no AS prescriptionNo, p.encounter_id AS encounterId,
                   p.patient_id AS patientId, pt.name AS patientName, p.dept_id AS deptId, d.name AS deptName,
                   p.doctor_id AS doctorId, u.real_name AS doctorName, p.type, p.status,
                   p.revision_no AS revisionNo, p.diagnosis_summary AS diagnosisSummary,
                   p.medical_advice AS medicalAdvice, p.submitted_at AS submittedAt,
                   p.reviewed_by AS reviewedBy, r.real_name AS reviewerName, p.reviewed_at AS reviewedAt,
                   p.reject_reason AS rejectReason, p.expires_at AS expiresAt, p.created_at AS createdAt,
                   p.version
            FROM prescription p
            JOIN patient pt ON pt.id = p.patient_id
            JOIN sys_department d ON d.id = p.dept_id
            JOIN sys_user u ON u.id = p.doctor_id
            LEFT JOIN sys_user r ON r.id = p.reviewed_by
            WHERE p.status = 'APPROVED' AND p.expires_at < #{now}
            """)
    List<Header> findExpired(@Param("now") Instant now);

    @Insert("""
            INSERT INTO prescription_revision (prescription_id, revision_no, content_json, content_hash,
                                               submitted_by, submitted_at)
            VALUES (#{prescriptionId}, #{revisionNo}, #{contentJson}, #{hash}, #{submittedBy}, #{at})
            """)
    int insertRevision(@Param("prescriptionId") long prescriptionId, @Param("revisionNo") int revisionNo,
            @Param("contentJson") String contentJson, @Param("hash") String hash,
            @Param("submittedBy") long submittedBy, @Param("at") Instant at);

    @Insert("""
            INSERT INTO prescription_review (prescription_id, revision_no, reviewer_id, decision, reason, reviewed_at)
            VALUES (#{prescriptionId}, #{revisionNo}, #{reviewerId}, #{decision}, #{reason}, #{at})
            """)
    int insertReview(@Param("prescriptionId") long prescriptionId, @Param("revisionNo") int revisionNo,
            @Param("reviewerId") long reviewerId, @Param("decision") String decision,
            @Param("reason") String reason, @Param("at") Instant at);

    @Insert("""
            INSERT INTO prescription_event (prescription_id, action, from_status, to_status, actor_id, reason, payload)
            VALUES (#{prescriptionId}, #{action}, #{fromStatus}, #{toStatus}, #{actorId}, #{reason}, #{payload})
            """)
    int insertEvent(@Param("prescriptionId") long prescriptionId, @Param("action") String action,
            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
            @Param("actorId") long actorId, @Param("reason") String reason, @Param("payload") String payload);

    @Select("""
            SELECT id, drug_id AS drugId, batch_no AS batchNo, expiry_date AS expiryDate, stock_qty AS stockQty,
                   reserved_qty AS reservedQty
            FROM drug_batch
            WHERE drug_id = #{drugId} AND status = 'ACTIVE' AND expiry_date >= CURRENT_DATE
              AND stock_qty > reserved_qty
            ORDER BY expiry_date, id
            """)
    List<Batch> batchesForDrug(long drugId);

    @Update("""
            UPDATE drug_batch SET reserved_qty = reserved_qty + #{quantity}, version = version + 1,
                updated_at = NOW(3)
            WHERE id = #{batchId} AND status = 'ACTIVE' AND stock_qty - reserved_qty >= #{quantity}
            """)
    int reserveBatch(@Param("batchId") long batchId, @Param("quantity") int quantity);

    @Update("""
            UPDATE drug_batch SET reserved_qty = reserved_qty - #{quantity}, version = version + 1,
                updated_at = NOW(3)
            WHERE id = #{batchId} AND reserved_qty >= #{quantity}
            """)
    int releaseBatch(@Param("batchId") long batchId, @Param("quantity") int quantity);

    @Select("""
            SELECT COALESCE(SUM(stock_qty - reserved_qty), 0) FROM drug_batch
            WHERE drug_id = #{drugId} AND status = 'ACTIVE' AND expiry_date >= CURRENT_DATE
            """)
    int availableStock(long drugId);

    @Insert("""
            INSERT INTO stock_reservation (prescription_id, prescription_item_id, drug_id, batch_id, quantity,
                                           status, idem_key)
            VALUES (#{prescriptionId}, #{itemId}, #{drugId}, #{batchId}, #{quantity}, 'HELD', #{idemKey})
            """)
    int insertReservation(@Param("prescriptionId") long prescriptionId, @Param("itemId") long itemId,
            @Param("drugId") long drugId, @Param("batchId") long batchId, @Param("quantity") int quantity,
            @Param("idemKey") String idemKey);

    @Select("""
            SELECT id, prescription_id AS prescriptionId, prescription_item_id AS itemId, drug_id AS drugId,
                   batch_id AS batchId, quantity, status
            FROM stock_reservation WHERE prescription_id = #{prescriptionId} AND status = 'HELD'
            """)
    List<Reservation> heldReservations(long prescriptionId);

    @Update("UPDATE stock_reservation SET status = 'RELEASED', updated_at = NOW(3) WHERE id = #{id} AND status = 'HELD'")
    int releaseReservation(long id);

    @Insert("""
            INSERT INTO stock_movement (movement_no, drug_id, batch_id, type, quantity, before_qty, after_qty,
                                        biz_type, biz_id, idem_key, operator_id, remark)
            VALUES (#{movementNo}, #{drugId}, #{batchId}, #{type}, #{quantity}, #{beforeQty}, #{afterQty},
                    #{bizType}, #{bizId}, #{idemKey}, #{operatorId}, #{remark})
            """)
    int insertMovement(@Param("movementNo") String movementNo, @Param("drugId") long drugId,
            @Param("batchId") long batchId, @Param("type") String type, @Param("quantity") int quantity,
            @Param("beforeQty") int beforeQty, @Param("afterQty") int afterQty,
            @Param("bizType") String bizType, @Param("bizId") long bizId, @Param("idemKey") String idemKey,
            @Param("operatorId") Long operatorId, @Param("remark") String remark);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
