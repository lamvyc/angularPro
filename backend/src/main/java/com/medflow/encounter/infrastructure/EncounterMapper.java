package com.medflow.encounter.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrRecordRow;
import com.medflow.encounter.domain.EmrTemplateRow;
import com.medflow.encounter.domain.EmrVersionRow;
import com.medflow.encounter.domain.EncounterRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EncounterMapper {

    @Select("""
            <script>
            SELECT e.id, e.encounter_no AS encounterNo, e.status, e.patient_id AS patientId,
                   p.name AS patientName, p.patient_no AS patientNo, e.dept_id AS deptId,
                   d.name AS deptName, e.doctor_id AS doctorId, u.real_name AS doctorName,
                   e.appointment_id AS appointmentId, a.appointment_date AS appointmentDate,
                   a.period, a.seq_no AS seqNo, e.chief_complaint AS chiefComplaint,
                   e.started_at AS startedAt, e.finished_at AS finishedAt, e.created_at AS createdAt
            FROM encounter e
            JOIN patient p ON p.id = e.patient_id
            JOIN sys_department d ON d.id = e.dept_id
            JOIN sys_user u ON u.id = e.doctor_id
            JOIN appointment a ON a.id = e.appointment_id
            WHERE e.status IN ('WAITING', 'IN_PROGRESS')
              AND a.appointment_date = #{date}
            <if test="doctorId != null">AND e.doctor_id = #{doctorId}</if>
            <if test="deptId != null">AND e.dept_id = #{deptId}</if>
            ORDER BY a.seq_no, e.id
            </script>
            """)
    List<EncounterRow> queue(@Param("date") LocalDate date, @Param("doctorId") Long doctorId,
            @Param("deptId") Long deptId);

    @Select("""
            SELECT e.id, e.encounter_no AS encounterNo, e.status, e.patient_id AS patientId,
                   p.name AS patientName, p.patient_no AS patientNo, e.dept_id AS deptId,
                   d.name AS deptName, e.doctor_id AS doctorId, u.real_name AS doctorName,
                   e.appointment_id AS appointmentId, a.appointment_date AS appointmentDate,
                   a.period, a.seq_no AS seqNo, e.chief_complaint AS chiefComplaint,
                   e.started_at AS startedAt, e.finished_at AS finishedAt, e.created_at AS createdAt
            FROM encounter e
            JOIN patient p ON p.id = e.patient_id
            JOIN sys_department d ON d.id = e.dept_id
            JOIN sys_user u ON u.id = e.doctor_id
            JOIN appointment a ON a.id = e.appointment_id
            WHERE e.id = #{id}
            """)
    Optional<EncounterRow> find(long id);

    @Update("""
            UPDATE encounter SET status = 'IN_PROGRESS', started_at = #{at}, version = version + 1,
                updated_at = NOW(3)
            WHERE id = #{id} AND status = 'WAITING'
            """)
    int start(@Param("id") long id, @Param("at") Instant at);

    @Update("""
            UPDATE encounter SET status = 'COMPLETED', finished_at = #{at}, version = version + 1,
                updated_at = NOW(3)
            WHERE id = #{id} AND status = 'IN_PROGRESS'
            """)
    int finish(@Param("id") long id, @Param("at") Instant at);

    @Update("""
            UPDATE encounter SET status = 'CANCELLED', cancel_reason = #{reason}, version = version + 1,
                updated_at = NOW(3)
            WHERE id = #{id} AND status IN ('WAITING', 'IN_PROGRESS')
            """)
    int cancel(@Param("id") long id, @Param("reason") String reason);

    @Update("""
            UPDATE appointment SET status = #{toStatus}, updated_at = NOW(3), version = version + 1
            WHERE id = #{appointmentId} AND status = #{fromStatus}
            """)
    int updateAppointmentStatus(@Param("appointmentId") long appointmentId,
            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus);

    @Insert("""
            INSERT INTO encounter_event (encounter_id, action, from_status, to_status, actor_id, reason, payload)
            VALUES (#{encounterId}, #{action}, #{fromStatus}, #{toStatus}, #{actorId}, #{reason}, #{payloadJson})
            """)
    int insertEvent(@Param("encounterId") long encounterId, @Param("action") String action,
            @Param("fromStatus") String fromStatus, @Param("toStatus") String toStatus,
            @Param("actorId") long actorId, @Param("reason") String reason,
            @Param("payloadJson") String payloadJson);

    @Select("""
            SELECT r.id, r.record_no AS recordNo, r.status, r.content_json AS contentJson, r.version,
                   r.updated_at AS updatedAt, r.template_id AS templateId,
                   r.template_version_id AS templateVersionId, t.name AS templateName,
                   v.schema_json AS schemaJson, r.signed_at AS signedAt, r.signed_by AS signedBy,
                   r.signature_hash AS signatureHash, r.amended_at AS amendedAt,
                   r.amend_reason AS amendReason
            FROM emr_record r
            JOIN emr_template t ON t.id = r.template_id
            JOIN emr_template_version v ON v.id = r.template_version_id
            WHERE r.encounter_id = #{encounterId}
            """)
    Optional<EmrRecordRow> findEmr(long encounterId);

    @Insert("""
            INSERT INTO emr_record (record_no, encounter_id, patient_id, doctor_id, template_id,
                                    template_version_id, status, content_json, created_by)
            VALUES (#{recordNo}, #{encounterId}, #{patientId}, #{doctorId}, #{templateId},
                    #{templateVersionId}, 'DRAFT', #{contentJson}, #{doctorId})
            """)
    int insertEmr(@Param("encounterId") long encounterId, @Param("patientId") long patientId,
            @Param("doctorId") long doctorId, @Param("recordNo") String recordNo,
            @Param("contentJson") String contentJson, @Param("templateId") long templateId,
            @Param("templateVersionId") long templateVersionId);

    @Update("""
            UPDATE emr_record SET content_json = #{contentJson}, version = version + 1,
                updated_by = #{operatorId}, updated_at = NOW(3)
            WHERE id = #{id} AND version = #{version} AND status = 'DRAFT'
            """)
    int updateEmr(@Param("id") long id, @Param("contentJson") String contentJson,
            @Param("version") int version, @Param("operatorId") Long operatorId);

    @Delete("DELETE FROM encounter_diagnosis WHERE encounter_id = #{encounterId}")
    int deleteDiagnoses(long encounterId);

    @Insert("""
            INSERT INTO encounter_diagnosis (encounter_id, diagnosis_code, diagnosis_name, type, note, sort_no,
                                             created_by)
            VALUES (#{encounterId}, #{code}, #{name}, #{type}, #{note}, #{sortNo}, #{operatorId})
            """)
    int insertDiagnosis(@Param("encounterId") long encounterId, @Param("code") String code,
            @Param("name") String name, @Param("type") String type, @Param("note") String note,
            @Param("sortNo") int sortNo, @Param("operatorId") Long operatorId);

    @Select("""
            SELECT id, diagnosis_code AS diagnosisCode, diagnosis_name AS diagnosisName, type, note,
                   sort_no AS sortNo
            FROM encounter_diagnosis WHERE encounter_id = #{encounterId} ORDER BY sort_no, id
            """)
    List<DiagnosisView> listDiagnoses(long encounterId);


    @Update("""
            UPDATE emr_record
            SET status = 'SIGNED', content_json = #{contentJson}, signed_at = #{at}, signed_by = #{signerId},
                signature_hash = #{hash}, version = version + 1, updated_by = #{signerId}, updated_at = NOW(3)
            WHERE id = #{id} AND version = #{version} AND status = 'DRAFT'
            """)
    int signEmr(@Param("id") long id, @Param("version") int version,
            @Param("contentJson") String contentJson, @Param("hash") String hash,
            @Param("signerId") long signerId, @Param("at") Instant at);

    @Update("""
            UPDATE emr_record
            SET status = 'AMENDED', content_json = #{contentJson}, signed_at = #{at}, signed_by = #{signerId},
                signature_hash = #{hash}, amended_at = #{at}, amend_reason = #{reason},
                version = version + 1, updated_by = #{signerId}, updated_at = NOW(3)
            WHERE id = #{id} AND version = #{version} AND status IN ('SIGNED', 'AMENDED')
            """)
    int amendEmr(@Param("id") long id, @Param("version") int version,
            @Param("contentJson") String contentJson, @Param("hash") String hash,
            @Param("signerId") long signerId, @Param("at") Instant at, @Param("reason") String reason);

    @Update("UPDATE emr_record_version SET status = 'SUPERSEDED' WHERE record_id = #{recordId} AND status = 'SIGNED'")
    int supersedeVersions(long recordId);

    @Insert("""
            INSERT INTO emr_record_version (record_id, version_no, content_json, content_hash, status,
                                            author_id, signed_by, signed_at, change_reason)
            VALUES (#{recordId}, #{versionNo}, #{contentJson}, #{hash}, #{status}, #{authorId},
                    #{signedBy}, #{signedAt}, #{changeReason})
            """)
    int insertVersion(@Param("recordId") long recordId, @Param("versionNo") int versionNo,
            @Param("contentJson") String contentJson, @Param("hash") String hash,
            @Param("status") String status, @Param("authorId") Long authorId,
            @Param("signedBy") Long signedBy, @Param("signedAt") Instant signedAt,
            @Param("changeReason") String changeReason);

    @Select("""
            SELECT v.version_no AS versionNo, v.status, v.content_json AS contentJson,
                   v.content_hash AS contentHash, v.signed_by AS signedBy, u.real_name AS signedByName,
                   v.signed_at AS signedAt, v.change_reason AS changeReason, v.created_at AS createdAt
            FROM emr_record_version v LEFT JOIN sys_user u ON u.id = v.signed_by
            WHERE v.record_id = #{recordId} ORDER BY v.version_no DESC
            """)
    List<EmrVersionRow> listVersions(long recordId);

    @Select("""
            SELECT v.version_no AS versionNo, v.status, v.content_json AS contentJson,
                   v.content_hash AS contentHash, v.signed_by AS signedBy, u.real_name AS signedByName,
                   v.signed_at AS signedAt, v.change_reason AS changeReason, v.created_at AS createdAt
            FROM emr_record_version v LEFT JOIN sys_user u ON u.id = v.signed_by
            WHERE v.record_id = #{recordId} AND v.version_no = #{versionNo}
            """)
    Optional<EmrVersionRow> findVersion(@Param("recordId") long recordId, @Param("versionNo") int versionNo);

    @Select("""
            SELECT t.id, t.code, t.name, t.scope_type AS scopeType, t.dept_id AS deptId,
                   t.current_version_id AS currentVersionId, v.version_no AS versionNo,
                   v.schema_json AS schemaJson, v.changelog, t.status, v.published_at AS publishedAt
            FROM emr_template t JOIN emr_template_version v ON v.id = t.current_version_id
            WHERE t.status = 'PUBLISHED'
              AND (t.scope_type = 'HOSPITAL' OR (t.scope_type = 'DEPT' AND t.dept_id = #{deptId}))
            ORDER BY CASE WHEN t.scope_type = 'DEPT' THEN 0 ELSE 1 END, t.id LIMIT 1
            """)
    Optional<EmrTemplateRow> findTemplateForDept(long deptId);

    @Select("""
            SELECT t.id, t.code, t.name, t.scope_type AS scopeType, t.dept_id AS deptId,
                   t.current_version_id AS currentVersionId, v.version_no AS versionNo,
                   v.schema_json AS schemaJson, v.changelog, t.status, v.published_at AS publishedAt
            FROM emr_template t JOIN emr_template_version v ON v.id = t.current_version_id
            WHERE t.id = #{id}
            """)
    Optional<EmrTemplateRow> findTemplate(long id);

    @Select("""
            SELECT t.id, t.code, t.name, t.scope_type AS scopeType, t.dept_id AS deptId,
                   t.current_version_id AS currentVersionId, v.version_no AS versionNo,
                   v.schema_json AS schemaJson, v.changelog, t.status, v.published_at AS publishedAt
            FROM emr_template t JOIN emr_template_version v ON v.id = t.current_version_id
            ORDER BY t.id
            """)
    List<EmrTemplateRow> listCurrentTemplates();

    @Select("""
            SELECT t.id, t.code, t.name, t.scope_type AS scopeType, t.dept_id AS deptId,
                   t.current_version_id AS currentVersionId, v.version_no AS versionNo,
                   v.schema_json AS schemaJson, v.changelog, v.status, v.published_at AS publishedAt
            FROM emr_template t JOIN emr_template_version v ON v.template_id = t.id
            WHERE t.id = #{templateId} ORDER BY v.version_no DESC
            """)
    List<EmrTemplateRow> listTemplateVersions(long templateId);

    @Insert("""
            INSERT INTO emr_template (code, name, scope_type, dept_id, status, created_by)
            VALUES (#{code}, #{name}, #{scopeType}, #{deptId}, 'PUBLISHED', #{operatorId})
            """)
    int insertTemplate(@Param("code") String code, @Param("name") String name,
            @Param("scopeType") String scopeType, @Param("deptId") Long deptId,
            @Param("operatorId") long operatorId);

    @Insert("""
            INSERT INTO emr_template_version (template_id, version_no, schema_json, changelog, status,
                                              published_by, published_at)
            VALUES (#{templateId}, #{versionNo}, #{schemaJson}, #{changelog}, 'PUBLISHED', #{operatorId}, #{at})
            """)
    int insertTemplateVersion(@Param("templateId") long templateId, @Param("versionNo") int versionNo,
            @Param("schemaJson") String schemaJson, @Param("changelog") String changelog,
            @Param("operatorId") long operatorId, @Param("at") Instant at);

    @Update("UPDATE emr_template SET current_version_id = #{versionId}, updated_by = #{operatorId}, updated_at = NOW(3) WHERE id = #{templateId}")
    int setTemplateCurrentVersion(@Param("templateId") long templateId, @Param("versionId") long versionId,
            @Param("operatorId") long operatorId);

    @Update("UPDATE emr_template SET status = 'ARCHIVED', updated_by = #{operatorId}, updated_at = NOW(3) WHERE id = #{templateId}")
    int archiveTemplate(@Param("templateId") long templateId, @Param("operatorId") long operatorId);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
