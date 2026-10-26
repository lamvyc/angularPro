package com.medflow.encounter.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.medflow.encounter.domain.DiagnosisView;
import com.medflow.encounter.domain.EmrRecordRow;
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
            SELECT id, record_no AS recordNo, status, content_json AS contentJson, version,
                   updated_at AS updatedAt
            FROM emr_record WHERE encounter_id = #{encounterId}
            """)
    Optional<EmrRecordRow> findEmr(long encounterId);

    @Insert("""
            INSERT INTO emr_record (record_no, encounter_id, patient_id, doctor_id, status, content_json, created_by)
            VALUES (#{recordNo}, #{encounterId}, #{patientId}, #{doctorId}, 'DRAFT', #{contentJson}, #{doctorId})
            """)
    int insertEmr(@Param("encounterId") long encounterId, @Param("patientId") long patientId,
            @Param("doctorId") long doctorId, @Param("recordNo") String recordNo,
            @Param("contentJson") String contentJson);

    @Update("""
            UPDATE emr_record SET content_json = #{contentJson}, version = version + 1,
                updated_by = #{operatorId}, updated_at = NOW(3)
            WHERE id = #{id} AND version = #{version}
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

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
}
