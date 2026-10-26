package com.medflow.pharmacy.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.pharmacy.domain.PharmacyModels.BatchView;
import com.medflow.pharmacy.domain.PharmacyModels.DrugAdmin;
import com.medflow.pharmacy.domain.PharmacyModels.MovementView;
import com.medflow.pharmacy.domain.PharmacyModels.ReservationView;
import com.medflow.pharmacy.domain.PharmacyModels.Summary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PharmacyMapper {

    @Select("""
            SELECT id, drug_code AS code, name, generic_name AS genericName, spec, unit,
                   dosage_form AS dosageForm, manufacturer, category, unit_price AS unitPrice,
                   prescription_type AS prescriptionType, status
            FROM drug ORDER BY id
            """)
    List<DrugAdmin> listDrugs();

    @Select("""
            SELECT id, drug_code AS code, name, generic_name AS genericName, spec, unit,
                   dosage_form AS dosageForm, manufacturer, category, unit_price AS unitPrice,
                   prescription_type AS prescriptionType, status
            FROM drug WHERE id = #{id}
            """)
    Optional<DrugAdmin> findDrug(long id);

    @Insert("""
            INSERT INTO drug (drug_code, name, generic_name, spec, unit, dosage_form, manufacturer, category,
                              unit_price, prescription_type, status)
            VALUES (#{code}, #{name}, #{genericName}, #{spec}, #{unit}, #{dosageForm}, #{manufacturer},
                    #{category}, #{price}, #{prescriptionType}, 'ENABLED')
            """)
    int insertDrug(@Param("code") String code, @Param("name") String name,
            @Param("genericName") String genericName, @Param("spec") String spec, @Param("unit") String unit,
            @Param("dosageForm") String dosageForm, @Param("manufacturer") String manufacturer,
            @Param("category") String category, @Param("price") BigDecimal price,
            @Param("prescriptionType") String prescriptionType);

    @Update("""
            UPDATE drug SET name = #{name}, generic_name = #{genericName}, spec = #{spec}, unit = #{unit},
                dosage_form = #{dosageForm}, manufacturer = #{manufacturer}, category = #{category},
                unit_price = #{price}, prescription_type = #{prescriptionType}
            WHERE id = #{id}
            """)
    int updateDrug(@Param("id") long id, @Param("name") String name,
            @Param("genericName") String genericName, @Param("spec") String spec, @Param("unit") String unit,
            @Param("dosageForm") String dosageForm, @Param("manufacturer") String manufacturer,
            @Param("category") String category, @Param("price") BigDecimal price,
            @Param("prescriptionType") String prescriptionType);

    @Update("UPDATE drug SET status = #{status} WHERE id = #{id}")
    int setDrugStatus(@Param("id") long id, @Param("status") String status);

    @Select("""
            <script>
            SELECT b.id, b.drug_id AS drugId, d.name AS drugName, b.batch_no AS batchNo,
                   b.production_date AS productionDate, b.expiry_date AS expiryDate,
                   b.stock_qty AS stockQty, b.reserved_qty AS reservedQty,
                   b.stock_qty - b.reserved_qty AS available, b.status
            FROM drug_batch b JOIN drug d ON d.id = b.drug_id
            <where><if test="drugId != null">b.drug_id = #{drugId}</if></where>
            ORDER BY b.expiry_date, b.id
            </script>
            """)
    List<BatchView> listBatches(@Param("drugId") Long drugId);

    @Select("""
            SELECT b.id, b.drug_id AS drugId, d.name AS drugName, b.batch_no AS batchNo,
                   b.production_date AS productionDate, b.expiry_date AS expiryDate,
                   b.stock_qty AS stockQty, b.reserved_qty AS reservedQty,
                   b.stock_qty - b.reserved_qty AS available, b.status
            FROM drug_batch b JOIN drug d ON d.id = b.drug_id WHERE b.id = #{id}
            """)
    Optional<BatchView> findBatch(long id);

    @Insert("""
            INSERT INTO drug_batch (drug_id, batch_no, production_date, expiry_date, stock_qty, reserved_qty, status)
            VALUES (#{drugId}, #{batchNo}, #{productionDate}, #{expiryDate}, #{quantity}, 0, 'ACTIVE')
            """)
    int insertBatch(@Param("drugId") long drugId, @Param("batchNo") String batchNo,
            @Param("productionDate") LocalDate productionDate, @Param("expiryDate") LocalDate expiryDate,
            @Param("quantity") int quantity);

    @Update("""
            UPDATE drug_batch SET stock_qty = stock_qty - #{quantity}, reserved_qty = reserved_qty - #{quantity},
                status = CASE WHEN stock_qty - #{quantity} = 0 THEN 'DEPLETED' ELSE status END,
                version = version + 1, updated_at = NOW(3)
            WHERE id = #{batchId} AND status = 'ACTIVE' AND stock_qty >= #{quantity}
              AND reserved_qty >= #{quantity}
            """)
    int consumeBatch(@Param("batchId") long batchId, @Param("quantity") int quantity);

    @Update("""
            UPDATE drug_batch SET stock_qty = stock_qty + #{quantity},
                status = CASE WHEN expiry_date < CURRENT_DATE THEN 'QUARANTINE'
                              WHEN status = 'DEPLETED' THEN 'ACTIVE' ELSE status END,
                version = version + 1, updated_at = NOW(3)
            WHERE id = #{batchId}
            """)
    int returnBatch(@Param("batchId") long batchId, @Param("quantity") int quantity);

    @Select("""
            SELECT d.id AS drugId, d.name AS drugName, COALESCE(SUM(b.stock_qty), 0) AS stockQty,
                   COALESCE(SUM(b.reserved_qty), 0) AS reservedQty,
                   COALESCE(SUM(b.stock_qty - b.reserved_qty), 0) AS available,
                   COUNT(b.id) AS batches
            FROM drug d LEFT JOIN drug_batch b ON b.drug_id = d.id
            GROUP BY d.id, d.name ORDER BY d.name
            """)
    List<Summary> summary();

    @Select("""
            SELECT m.id, m.movement_no AS movementNo, d.name AS drugName, b.batch_no AS batchNo, m.type,
                   m.quantity, m.before_qty AS beforeQty, m.after_qty AS afterQty, m.biz_type AS bizType,
                   m.biz_id AS bizId, m.remark, m.created_at AS createdAt
            FROM stock_movement m JOIN drug d ON d.id = m.drug_id JOIN drug_batch b ON b.id = m.batch_id
            ORDER BY m.id DESC LIMIT #{limit}
            """)
    List<MovementView> movements(int limit);

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

    @Select("""
            <script>
            SELECT r.id, r.prescription_id AS prescriptionId, r.prescription_item_id AS itemId,
                   r.drug_id AS drugId, d.name AS drugName, r.batch_id AS batchId, b.batch_no AS batchNo,
                   b.expiry_date AS expiryDate, r.quantity, r.returned_qty AS returnedQty, r.status
            FROM stock_reservation r
            JOIN drug d ON d.id = r.drug_id JOIN drug_batch b ON b.id = r.batch_id
            WHERE r.prescription_id = #{prescriptionId}
            <if test="status != null and status != ''">AND r.status = #{status}</if>
            ORDER BY r.id
            </script>
            """)
    List<ReservationView> reservations(@Param("prescriptionId") long prescriptionId,
            @Param("status") String status);

    @Select("""
            SELECT r.id, r.prescription_id AS prescriptionId, r.prescription_item_id AS itemId,
                   r.drug_id AS drugId, d.name AS drugName, r.batch_id AS batchId, b.batch_no AS batchNo,
                   b.expiry_date AS expiryDate, r.quantity, r.returned_qty AS returnedQty, r.status
            FROM stock_reservation r JOIN drug d ON d.id = r.drug_id JOIN drug_batch b ON b.id = r.batch_id
            WHERE r.id = #{id}
            """)
    Optional<ReservationView> findReservation(long id);

    @Update("UPDATE stock_reservation SET status = 'CONSUMED', updated_at = NOW(3) WHERE id = #{id} AND status = 'HELD'")
    int markReservationConsumed(long id);

    @Update("""
            UPDATE stock_reservation SET returned_qty = returned_qty + #{quantity}, updated_at = NOW(3)
            WHERE id = #{id} AND returned_qty + #{quantity} <= quantity
            """)
    int addReturned(@Param("id") long id, @Param("quantity") int quantity);

    @Update("""
            UPDATE prescription SET status = 'DISPENSED', dispensed_by = #{operatorId}, dispensed_at = #{at},
                updated_by = #{operatorId}, updated_at = NOW(3), version = version + 1
            WHERE id = #{id} AND status = 'APPROVED'
            """)
    int dispensePrescription(@Param("id") long id, @Param("operatorId") long operatorId,
            @Param("at") Instant at);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
