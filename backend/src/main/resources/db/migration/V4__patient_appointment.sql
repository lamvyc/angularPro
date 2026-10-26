-- MedFlow V4: patients, schedules, slots, appointments, charges, refunds, minimal encounter.

CREATE TABLE patient (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    patient_no     VARCHAR(32)     NOT NULL,
    name           VARCHAR(50)     NOT NULL,
    gender         VARCHAR(8)      NOT NULL DEFAULT 'UNKNOWN',
    birth_date     DATE            NOT NULL,
    id_card_hash   CHAR(64)        NULL,
    id_card_masked VARCHAR(32)     NULL,
    phone          VARCHAR(20)     NULL,
    address        VARCHAR(255)    NULL,
    allergy_history VARCHAR(500)   NULL,
    status         VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by     BIGINT UNSIGNED NULL,
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by     BIGINT UNSIGNED NULL,
    version        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_no (patient_no),
    UNIQUE KEY uk_patient_id_card (id_card_hash),
    KEY idx_patient_name_birth (name, birth_date),
    KEY idx_patient_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者主档';

CREATE TABLE doctor_schedule (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    dept_id         BIGINT UNSIGNED NOT NULL,
    doctor_id       BIGINT UNSIGNED NOT NULL,
    schedule_date   DATE            NOT NULL,
    period          VARCHAR(8)      NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    room            VARCHAR(32)     NULL,
    registration_fee DECIMAL(10, 2) NOT NULL DEFAULT 20.00,
    status          VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
    published_at    DATETIME(3)     NULL,
    published_by    BIGINT UNSIGNED NULL,
    active_key      VARCHAR(96) GENERATED ALWAYS AS (
        CASE WHEN status IN ('DRAFT', 'PUBLISHED')
             THEN CONCAT(doctor_id, '-', schedule_date, '-', period) END
    ) STORED,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      BIGINT UNSIGNED NULL,
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by      BIGINT UNSIGNED NULL,
    version         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_active (active_key),
    KEY idx_schedule_dept_date (dept_id, schedule_date, status),
    KEY idx_schedule_doctor_date (doctor_id, schedule_date),
    CONSTRAINT fk_schedule_dept FOREIGN KEY (dept_id) REFERENCES sys_department (id),
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '医生排班';

CREATE TABLE schedule_slot (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    schedule_id  BIGINT UNSIGNED NOT NULL,
    seq_no       INT             NOT NULL,
    start_time   TIME            NOT NULL,
    end_time     TIME            NOT NULL,
    capacity     INT             NOT NULL,
    booked_count INT             NOT NULL DEFAULT 0,
    status       VARCHAR(16)     NOT NULL DEFAULT 'OPEN',
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   BIGINT UNSIGNED NULL,
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by   BIGINT UNSIGNED NULL,
    version      INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot_schedule_seq (schedule_id, seq_no),
    KEY idx_slot_schedule_status (schedule_id, status),
    CONSTRAINT fk_slot_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedule (id),
    CONSTRAINT chk_slot_count CHECK (booked_count >= 0 AND booked_count <= capacity)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '号源';

CREATE TABLE appointment (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    appointment_no      VARCHAR(32)     NOT NULL,
    patient_id          BIGINT UNSIGNED NOT NULL,
    dept_id             BIGINT UNSIGNED NOT NULL,
    doctor_id           BIGINT UNSIGNED NOT NULL,
    schedule_id         BIGINT UNSIGNED NOT NULL,
    slot_id             BIGINT UNSIGNED NOT NULL,
    appointment_date    DATE            NOT NULL,
    period              VARCHAR(8)      NOT NULL,
    seq_no              INT             NOT NULL,
    status              VARCHAR(32)     NOT NULL,
    source              VARCHAR(16)     NOT NULL DEFAULT 'WINDOW',
    booked_by           BIGINT UNSIGNED NULL,
    checked_in_at       DATETIME(3)     NULL,
    cancelled_at        DATETIME(3)     NULL,
    cancel_reason       VARCHAR(255)    NULL,
    encounter_id        BIGINT UNSIGNED NULL,
    active_patient_id   BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('BOOKED', 'CHECKED_IN', 'IN_CONSULTATION') THEN patient_id END
    ) STORED,
    active_doctor_day_key VARCHAR(128) GENERATED ALWAYS AS (
        CASE WHEN status IN ('BOOKED', 'CHECKED_IN', 'IN_CONSULTATION')
             THEN CONCAT(patient_id, '-', doctor_id, '-', appointment_date, '-', period) END
    ) STORED,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          BIGINT UNSIGNED NULL,
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by          BIGINT UNSIGNED NULL,
    version             INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    UNIQUE KEY uk_appointment_slot_patient (slot_id, active_patient_id),
    UNIQUE KEY uk_appointment_doctor_day (active_doctor_day_key),
    KEY idx_appointment_patient_date (patient_id, appointment_date),
    KEY idx_appointment_doctor_status (doctor_id, appointment_date, status),
    KEY idx_appointment_dept_date (dept_id, appointment_date),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_appointment_dept FOREIGN KEY (dept_id) REFERENCES sys_department (id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user (id),
    CONSTRAINT fk_appointment_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedule (id),
    CONSTRAINT fk_appointment_slot FOREIGN KEY (slot_id) REFERENCES schedule_slot (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约';

CREATE TABLE charge_order (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no       VARCHAR(32)     NOT NULL,
    biz_type       VARCHAR(32)     NOT NULL,
    biz_id         BIGINT UNSIGNED NOT NULL,
    patient_id     BIGINT UNSIGNED NOT NULL,
    dept_id        BIGINT UNSIGNED NOT NULL,
    amount         DECIMAL(10, 2)  NOT NULL,
    status         VARCHAR(32)     NOT NULL,
    payment_method VARCHAR(16)     NULL,
    paid_at        DATETIME(3)     NULL,
    cashier_id     BIGINT UNSIGNED NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by     BIGINT UNSIGNED NULL,
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by     BIGINT UNSIGNED NULL,
    version        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_charge_no (order_no),
    UNIQUE KEY uk_charge_biz (biz_type, biz_id),
    KEY idx_charge_patient (patient_id, created_at),
    KEY idx_charge_status (status),
    CONSTRAINT fk_charge_patient FOREIGN KEY (patient_id) REFERENCES patient (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费单';

CREATE TABLE refund_record (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_no   VARCHAR(32)     NOT NULL,
    charge_id   BIGINT UNSIGNED NOT NULL,
    biz_type    VARCHAR(32)     NOT NULL,
    biz_id      BIGINT UNSIGNED NOT NULL,
    amount      DECIMAL(10, 2)  NOT NULL,
    reason      VARCHAR(255)    NULL,
    status      VARCHAR(16)     NOT NULL,
    retry_count INT             NOT NULL DEFAULT 0,
    last_error  VARCHAR(255)    NULL,
    operator_id BIGINT UNSIGNED NULL,
    refunded_at DATETIME(3)     NULL,
    idem_key    VARCHAR(96)     NOT NULL,
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_refund_idem (idem_key),
    KEY idx_refund_status (status, retry_count),
    CONSTRAINT fk_refund_charge FOREIGN KEY (charge_id) REFERENCES charge_order (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '退款记录';

CREATE TABLE encounter (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    encounter_no   VARCHAR(32)     NOT NULL,
    appointment_id BIGINT UNSIGNED NOT NULL,
    patient_id     BIGINT UNSIGNED NOT NULL,
    dept_id        BIGINT UNSIGNED NOT NULL,
    doctor_id      BIGINT UNSIGNED NOT NULL,
    status         VARCHAR(32)     NOT NULL,
    chief_complaint VARCHAR(500)   NULL,
    started_at     DATETIME(3)     NULL,
    finished_at    DATETIME(3)     NULL,
    cancel_reason  VARCHAR(255)    NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by     BIGINT UNSIGNED NULL,
    updated_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by     BIGINT UNSIGNED NULL,
    version        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_encounter_no (encounter_no),
    UNIQUE KEY uk_encounter_appointment (appointment_id),
    KEY idx_encounter_doctor_day (doctor_id, created_at, status),
    KEY idx_encounter_dept_status (dept_id, status),
    CONSTRAINT fk_encounter_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id),
    CONSTRAINT fk_encounter_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_encounter_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '就诊（M3 扩展）';

-- 菜单权限：预约/排班入口
INSERT INTO sys_permission (id, code, name, type, route_path, icon, sort_no) VALUES
    (49, 'menu:appointment', '预约挂号', 'MENU', '/appointments', 'calendar', 40),
    (50, 'menu:schedule', '排班管理', 'MENU', '/schedules', 'clipboard', 41);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (2, 49), (2, 50), (3, 49), (3, 50), (5, 49), (5, 50);

-- 全科护士，用于跨科室签到与演示（M1 种子只有皮肤科护士）
INSERT INTO sys_user (id, username, password_hash, real_name, employee_no, dept_id, title, phone, status)
VALUES (8, 'nurse3', '$2y$12$1x7tZ4nsk9XwUjOJJ4MNEe2eU7NL2Bkw28rbYALXiGv9e67rcXarK',
        '刘敏', 'N0003', 2, '护士', '13800000008', 'ENABLED');
INSERT INTO sys_user_role (user_id, role_id) VALUES (8, 3);
