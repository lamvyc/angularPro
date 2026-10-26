-- Synthetic demo data for local/docker only. Loaded via spring.flyway.locations in local/docker profiles.
-- Fixed high IDs keep this script isolated from real/test data; INSERT IGNORE / ON DUPLICATE make it re-runnable.

SET @today = CURDATE();
SET @tomorrow = DATE_ADD(CURDATE(), INTERVAL 1 DAY);

-- Ensure demo schedules, reusing an existing active schedule for the same doctor/date/period.
INSERT INTO doctor_schedule (dept_id, doctor_id, schedule_date, period, start_time, end_time, room,
                             registration_fee, status, published_at, published_by, created_by)
VALUES (2, 3, @today, 'AM', '08:00:00', '12:00:00', '全科 1 诊室', 20.00, 'PUBLISHED', NOW(3), 3, 3)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @sch_d2_today_am = LAST_INSERT_ID();

INSERT INTO doctor_schedule (dept_id, doctor_id, schedule_date, period, start_time, end_time, room,
                             registration_fee, status, published_at, published_by, created_by)
VALUES (2, 3, @tomorrow, 'AM', '08:00:00', '12:00:00', '全科 1 诊室', 20.00, 'PUBLISHED', NOW(3), 3, 3)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @sch_d2_tomorrow_am = LAST_INSERT_ID();

INSERT INTO doctor_schedule (dept_id, doctor_id, schedule_date, period, start_time, end_time, room,
                             registration_fee, status, published_at, published_by, created_by)
VALUES (1, 2, @today, 'PM', '14:00:00', '17:00:00', '皮肤科 2 诊室', 25.00, 'PUBLISHED', NOW(3), 2, 2)
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @sch_d1_today_pm = LAST_INSERT_ID();

-- Demo slots use seq 90+ so they do not collide with schedules created interactively.
INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_today_am, 90, '08:00:00', '08:30:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_1 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_today_am, 91, '08:30:00', '09:00:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_2 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_today_am, 92, '09:00:00', '09:30:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_3 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_today_am, 93, '09:30:00', '10:00:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_4 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_tomorrow_am, 90, '08:00:00', '08:30:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_tomorrow_1 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d2_tomorrow_am, 91, '08:30:00', '09:00:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d2_tomorrow_2 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d1_today_pm, 90, '14:00:00', '14:30:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d1_1 = LAST_INSERT_ID();

INSERT INTO schedule_slot (schedule_id, seq_no, start_time, end_time, capacity, status)
VALUES (@sch_d1_today_pm, 91, '14:30:00', '15:00:00', 1, 'OPEN')
ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);
SET @slot_d1_2 = LAST_INSERT_ID();

-- Patients
INSERT IGNORE INTO patient (id, patient_no, name, gender, birth_date, id_card_masked, phone, address,
                            allergy_history, status)
VALUES
    (9001, 'DEMO-P-001', '王明', 'MALE', '1988-03-12', '310101********1234', '13900009001', '上海合成地址 1 号', '无', 'ACTIVE'),
    (9002, 'DEMO-P-002', '李华', 'FEMALE', '1992-07-08', '310101********5678', '13900009002', '上海合成地址 2 号', '青霉素过敏', 'ACTIVE'),
    (9003, 'DEMO-P-003', '张伟', 'MALE', '1979-11-21', '310101********9012', '13900009003', '上海合成地址 3 号', '无', 'ACTIVE'),
    (9004, 'DEMO-P-004', '赵敏', 'FEMALE', '1995-01-16', '310101********3456', '13900009004', '上海合成地址 4 号', '磺胺过敏', 'ACTIVE'),
    (9005, 'DEMO-P-005', '陈静', 'FEMALE', '1985-05-30', '310101********7890', '13900009005', '上海合成地址 5 号', '无', 'ACTIVE'),
    (9006, 'DEMO-P-006', '刘洋', 'MALE', '1998-09-03', '310101********2345', '13900009006', '上海合成地址 6 号', '无', 'ACTIVE');

-- Appointments: completed / checked-in / in-consultation / booked / cancelled
INSERT IGNORE INTO appointment (id, appointment_no, patient_id, dept_id, doctor_id, schedule_id, slot_id,
                                appointment_date, period, seq_no, status, source, booked_by, checked_in_at,
                                cancelled_at, cancel_reason)
VALUES
    (9001, 'APDEMO0001', 9001, 2, 3, @sch_d2_today_am, @slot_d2_1, @today, 'AM', 1, 'COMPLETED', 'WINDOW', 6, DATE_ADD(NOW(3), INTERVAL -2 HOUR), NULL, NULL),
    (9002, 'APDEMO0002', 9002, 2, 3, @sch_d2_today_am, @slot_d2_2, @today, 'AM', 2, 'CHECKED_IN', 'WINDOW', 6, DATE_ADD(NOW(3), INTERVAL -1 HOUR), NULL, NULL),
    (9003, 'APDEMO0003', 9003, 2, 3, @sch_d2_today_am, @slot_d2_3, @today, 'AM', 3, 'IN_CONSULTATION', 'WINDOW', 6, DATE_ADD(NOW(3), INTERVAL -30 MINUTE), NULL, NULL),
    (9004, 'APDEMO0004', 9004, 2, 3, @sch_d2_tomorrow_am, @slot_d2_tomorrow_1, @tomorrow, 'AM', 1, 'BOOKED', 'WINDOW', 6, NULL, NULL, NULL),
    (9005, 'APDEMO0005', 9005, 1, 2, @sch_d1_today_pm, @slot_d1_1, @today, 'PM', 1, 'CHECKED_IN', 'WINDOW', 6, DATE_ADD(NOW(3), INTERVAL -20 MINUTE), NULL, NULL),
    (9006, 'APDEMO0006', 9006, 2, 3, @sch_d2_today_am, @slot_d2_4, @today, 'AM', 4, 'CANCELLED', 'WINDOW', 6, NULL, DATE_ADD(NOW(3), INTERVAL -3 HOUR), '患者临时取消');

-- Registration charges
INSERT IGNORE INTO charge_order (id, order_no, biz_type, biz_id, patient_id, dept_id, amount, status,
                                 payment_method, paid_at, cashier_id)
VALUES
    (9001, 'CHDEMO0001', 'REGISTRATION', 9001, 9001, 2, 20.00, 'PAID', 'CASH', DATE_ADD(NOW(3), INTERVAL -2 HOUR), 6),
    (9002, 'CHDEMO0002', 'REGISTRATION', 9002, 9002, 2, 20.00, 'UNPAID', NULL, NULL, NULL),
    (9003, 'CHDEMO0003', 'REGISTRATION', 9003, 9003, 2, 20.00, 'PAID', 'WECHAT', DATE_ADD(NOW(3), INTERVAL -1 HOUR), 6),
    (9004, 'CHDEMO0004', 'REGISTRATION', 9004, 9004, 2, 20.00, 'UNPAID', NULL, NULL, NULL),
    (9005, 'CHDEMO0005', 'REGISTRATION', 9005, 9005, 1, 25.00, 'UNPAID', NULL, NULL, NULL);

-- Encounters
INSERT IGNORE INTO encounter (id, encounter_no, appointment_id, patient_id, dept_id, doctor_id, status,
                              chief_complaint, started_at, finished_at)
VALUES
    (9001, 'ENDEMO0001', 9001, 9001, 2, 3, 'COMPLETED', '发热三天', DATE_ADD(NOW(3), INTERVAL -2 HOUR), DATE_ADD(NOW(3), INTERVAL -1 HOUR)),
    (9002, 'ENDEMO0002', 9002, 9002, 2, 3, 'WAITING', '咳嗽两天', NULL, NULL),
    (9003, 'ENDEMO0003', 9003, 9003, 2, 3, 'IN_PROGRESS', '咽痛一天', DATE_ADD(NOW(3), INTERVAL -30 MINUTE), NULL),
    (9005, 'ENDEMO0004', 9005, 9005, 1, 2, 'WAITING', '皮疹三天', NULL, NULL);

UPDATE appointment SET encounter_id = 9001 WHERE id = 9001;
UPDATE appointment SET encounter_id = 9002 WHERE id = 9002;
UPDATE appointment SET encounter_id = 9003 WHERE id = 9003;
UPDATE appointment SET encounter_id = 9005 WHERE id = 9005;

-- Signed and draft EMR records
INSERT IGNORE INTO emr_record (id, record_no, encounter_id, patient_id, doctor_id, template_id,
                               template_version_id, status, content_json, signed_at, signed_by,
                               signature_hash, version, created_by)
VALUES
    (9001, 'EMRDEMO0001', 9001, 9001, 3, 1, 1, 'SIGNED',
     '{"chiefComplaint":"发热三天","fever":true,"temperature":38.5,"onsetDate":"2026-10-20","presentIllness":"受凉后发热，伴咽痛","pastHistory":"无特殊","physicalExam":"咽部充血，双肺呼吸音清","auxiliaryExam":"血常规未见明显异常","treatmentPlan":"对症治疗，多饮水"}',
     DATE_ADD(NOW(3), INTERVAL -1 HOUR), 3, SHA2('demo-emr-9001', 256), 1, 3),
    (9003, 'EMRDEMO0002', 9003, 9003, 3, 1, 1, 'DRAFT',
     '{"chiefComplaint":"咽痛一天","fever":false,"presentIllness":"晨起咽痛，无发热"}',
     NULL, NULL, NULL, 0, 3);

INSERT IGNORE INTO emr_record_version (id, record_id, version_no, content_json, content_hash, status,
                                       author_id, signed_by, signed_at)
SELECT 9001, 9001, 1, content_json, SHA2(content_json, 256), 'SIGNED', 3, 3, signed_at
FROM emr_record WHERE id = 9001;

INSERT IGNORE INTO encounter_diagnosis (id, encounter_id, diagnosis_code, diagnosis_name, type, note, sort_no, created_by)
VALUES
    (9001, 9001, 'J06.9', '急性上呼吸道感染', 'PRIMARY', '病毒性', 0, 3),
    (9002, 9003, 'J02.9', '急性咽炎', 'PRIMARY', NULL, 0, 3);

-- Demo stock batches, isolated from interactive FEFO batches.
INSERT IGNORE INTO drug_batch (id, drug_id, batch_no, production_date, expiry_date, stock_qty, reserved_qty, status)
VALUES
    (9001, 1, 'DEMO-AMOX-A', '2026-04-01', '2027-03-31', 50, 0, 'ACTIVE'),
    (9002, 1, 'DEMO-AMOX-B', '2025-12-01', '2026-11-30', 39, 2, 'ACTIVE'),
    (9003, 2, 'DEMO-IBU-A', '2026-05-01', '2027-08-31', 40, 0, 'ACTIVE');

INSERT IGNORE INTO stock_movement (movement_no, drug_id, batch_id, type, quantity, before_qty, after_qty,
                                   biz_type, biz_id, idem_key, operator_id, remark)
VALUES
    ('DEMO-MV-IN-1', 1, 9001, 'IN', 50, 0, 50, 'BATCH', 9001, 'DEMO-IN-9001', 5, '演示批次入库'),
    ('DEMO-MV-IN-2', 1, 9002, 'IN', 40, 0, 40, 'BATCH', 9002, 'DEMO-IN-9002', 5, '演示批次入库'),
    ('DEMO-MV-IN-3', 2, 9003, 'IN', 40, 0, 40, 'BATCH', 9003, 'DEMO-IN-9003', 5, '演示批次入库'),
    ('DEMO-MV-RES-1', 1, 9002, 'RESERVE', 2, 40, 38, 'PRESCRIPTION', 9002, 'DEMO-RES-9002', 5, '审方通过 FEFO 预占'),
    ('DEMO-MV-DISP-1', 1, 9002, 'DISPENSE', 1, 38, 37, 'PRESCRIPTION', 9003, 'DEMO-DISP-9003', 5, '发药扣减');

-- Prescriptions covering review queue, approved-held, dispensed and rejected states.
INSERT IGNORE INTO prescription (id, prescription_no, encounter_id, patient_id, dept_id, doctor_id, type,
                                 status, revision_no, diagnosis_summary, medical_advice, submitted_at,
                                 reviewed_by, reviewed_at, reject_reason, dispensed_by, dispensed_at,
                                 cancel_reason, expires_at, created_by)
VALUES
    (9001, 'RXDEMO0001', 9003, 9003, 2, 3, 'WESTERN', 'SUBMITTED', 1, '急性咽炎', '饭后服用', DATE_ADD(NOW(3), INTERVAL -20 MINUTE), NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3),
    (9002, 'RXDEMO0002', 9003, 9003, 2, 3, 'WESTERN', 'APPROVED', 1, '急性咽炎', '饭后服用', DATE_ADD(NOW(3), INTERVAL -25 MINUTE), 5, DATE_ADD(NOW(3), INTERVAL -10 MINUTE), NULL, NULL, NULL, NULL, DATE_ADD(NOW(3), INTERVAL 72 HOUR), 3),
    (9003, 'RXDEMO0003', 9001, 9001, 2, 3, 'WESTERN', 'DISPENSED', 1, '急性上呼吸道感染', '对症治疗', DATE_ADD(NOW(3), INTERVAL -1 HOUR), 5, DATE_ADD(NOW(3), INTERVAL -50 MINUTE), NULL, 5, DATE_ADD(NOW(3), INTERVAL -40 MINUTE), NULL, DATE_ADD(NOW(3), INTERVAL 72 HOUR), 3),
    (9004, 'RXDEMO0004', 9001, 9001, 2, 3, 'WESTERN', 'REJECTED', 1, '急性上呼吸道感染', '退热', DATE_ADD(NOW(3), INTERVAL -1 HOUR), 5, DATE_ADD(NOW(3), INTERVAL -45 MINUTE), '剂量与年龄不符，请调整', NULL, NULL, NULL, NULL, 3),
    (9005, 'RXDEMO0005', 9003, 9003, 2, 3, 'WESTERN', 'DRAFT', 0, '急性咽炎', '含服', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3);

INSERT IGNORE INTO prescription_item (id, prescription_id, drug_id, drug_name, spec, dosage, frequency, route,
                                      days, quantity, unit, unit_price, amount, note, sort_no)
VALUES
    (9001, 9001, 2, '布洛芬片', '0.4g*20片', '1片', '每日3次', '口服', 3, 1, '盒', 12.00, 12.00, NULL, 0),
    (9002, 9002, 1, '阿莫西林胶囊', '0.5g*24粒', '1粒', '每日3次', '口服', 5, 2, '盒', 18.50, 37.00, NULL, 0),
    (9003, 9003, 1, '阿莫西林胶囊', '0.5g*24粒', '1粒', '每日3次', '口服', 3, 1, '盒', 18.50, 18.50, NULL, 0),
    (9004, 9004, 2, '布洛芬片', '0.4g*20片', '2片', '每日3次', '口服', 3, 1, '盒', 12.00, 12.00, '退热', 0),
    (9005, 9005, 3, '沙丁胺醇气雾剂', '100μg*200揿', '1揿', '按需', '吸入', 7, 1, '支', 35.00, 35.00, NULL, 0);

INSERT IGNORE INTO prescription_revision (id, prescription_id, revision_no, content_json, content_hash,
                                          submitted_by, submitted_at)
VALUES
    (9001, 9001, 1, '{"items":[{"drugId":2,"quantity":1}]}', SHA2('rx-demo-9001', 256), 3, DATE_ADD(NOW(3), INTERVAL -20 MINUTE)),
    (9002, 9002, 1, '{"items":[{"drugId":1,"quantity":2}]}', SHA2('rx-demo-9002', 256), 3, DATE_ADD(NOW(3), INTERVAL -25 MINUTE)),
    (9003, 9003, 1, '{"items":[{"drugId":1,"quantity":1}]}', SHA2('rx-demo-9003', 256), 3, DATE_ADD(NOW(3), INTERVAL -1 HOUR)),
    (9004, 9004, 1, '{"items":[{"drugId":2,"quantity":1}]}', SHA2('rx-demo-9004', 256), 3, DATE_ADD(NOW(3), INTERVAL -1 HOUR));

INSERT IGNORE INTO prescription_review (id, prescription_id, revision_no, reviewer_id, decision, reason, reviewed_at)
VALUES
    (9001, 9002, 1, 5, 'APPROVED', NULL, DATE_ADD(NOW(3), INTERVAL -10 MINUTE)),
    (9002, 9003, 1, 5, 'APPROVED', NULL, DATE_ADD(NOW(3), INTERVAL -50 MINUTE)),
    (9003, 9004, 1, 5, 'REJECTED', '剂量与年龄不符，请调整', DATE_ADD(NOW(3), INTERVAL -45 MINUTE));

INSERT IGNORE INTO stock_reservation (id, prescription_id, prescription_item_id, drug_id, batch_id, quantity,
                                      returned_qty, status, idem_key)
VALUES
    (9001, 9002, 9002, 1, 9002, 2, 0, 'HELD', 'DEMO-RES-9002'),
    (9002, 9003, 9003, 1, 9002, 1, 0, 'CONSUMED', 'DEMO-DISP-9003');

-- Events/audit are intentionally omitted: they are generated by real actions, not demo seed.

-- Reflect active demo appointments in slot occupancy for the UI.
UPDATE schedule_slot SET booked_count = 1
WHERE id IN (@slot_d2_2, @slot_d2_3, @slot_d1_1, @slot_d2_tomorrow_1)
  AND booked_count = 0;

-- Keep demo stock aligned if an earlier demo seed version reserved on the later-expiry batch.
UPDATE stock_reservation SET batch_id = 9002 WHERE id = 9001;
UPDATE drug_batch SET reserved_qty = 0 WHERE id = 9001;
UPDATE drug_batch SET reserved_qty = 2 WHERE id = 9002;
UPDATE stock_movement SET batch_id = 9002, before_qty = 40, after_qty = 38
WHERE movement_no = 'DEMO-MV-RES-1';
UPDATE stock_movement SET batch_id = 9002, before_qty = 38, after_qty = 37
WHERE movement_no = 'DEMO-MV-DISP-1';
