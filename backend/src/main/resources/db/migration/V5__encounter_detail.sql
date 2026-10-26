-- MedFlow V5: encounter diagnosis/events, EMR draft record, workstation menu.
-- Also adds an atomic per-schedule sequence for appointment display numbers.

ALTER TABLE doctor_schedule
    ADD COLUMN next_seq INT NOT NULL DEFAULT 0 AFTER published_by;

CREATE TABLE encounter_diagnosis (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    encounter_id        BIGINT UNSIGNED NOT NULL,
    diagnosis_code      VARCHAR(32)     NOT NULL,
    diagnosis_name      VARCHAR(100)    NOT NULL,
    type                VARCHAR(16)     NOT NULL,
    note                VARCHAR(255)    NULL,
    sort_no             INT             NOT NULL DEFAULT 0,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          BIGINT UNSIGNED NULL,
    primary_encounter_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN type = 'PRIMARY' THEN encounter_id END
    ) STORED,
    PRIMARY KEY (id),
    KEY idx_diag_encounter (encounter_id, sort_no),
    UNIQUE KEY uk_diag_primary (primary_encounter_id),
    CONSTRAINT fk_diag_encounter FOREIGN KEY (encounter_id) REFERENCES encounter (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '就诊诊断';

CREATE TABLE encounter_event (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    encounter_id BIGINT UNSIGNED NOT NULL,
    action       VARCHAR(32)     NOT NULL,
    from_status  VARCHAR(32)     NULL,
    to_status    VARCHAR(32)     NULL,
    actor_id     BIGINT UNSIGNED NULL,
    reason       VARCHAR(255)    NULL,
    payload      JSON            NULL,
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_event_encounter (encounter_id, created_at),
    CONSTRAINT fk_event_encounter FOREIGN KEY (encounter_id) REFERENCES encounter (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '就诊事件（追加）';

CREATE TABLE emr_record (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    record_no    VARCHAR(32)     NOT NULL,
    encounter_id BIGINT UNSIGNED NOT NULL,
    patient_id   BIGINT UNSIGNED NOT NULL,
    doctor_id    BIGINT UNSIGNED NOT NULL,
    status       VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
    content_json JSON            NOT NULL,
    signed_at    DATETIME(3)     NULL,
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by   BIGINT UNSIGNED NULL,
    updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by   BIGINT UNSIGNED NULL,
    version      INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emr_no (record_no),
    UNIQUE KEY uk_emr_encounter (encounter_id),
    KEY idx_emr_doctor (doctor_id, updated_at),
    CONSTRAINT fk_emr_encounter FOREIGN KEY (encounter_id) REFERENCES encounter (id),
    CONSTRAINT fk_emr_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_emr_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病历记录（M3 草稿，M4 扩展版本/签署）';

INSERT INTO sys_permission (id, code, name, type, route_path, icon, sort_no) VALUES
    (51, 'menu:workstation', '医生工作站', 'MENU', '/workstation', 'pulse', 25);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (2, 51), (3, 51);
