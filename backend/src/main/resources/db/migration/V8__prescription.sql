-- MedFlow V8: prescriptions, reviews, revisions, minimal drug/batch stock and FEFO reservations.

CREATE TABLE drug (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    drug_code         VARCHAR(32)     NOT NULL,
    name              VARCHAR(100)    NOT NULL,
    spec              VARCHAR(100)    NOT NULL,
    unit              VARCHAR(16)     NOT NULL,
    dosage_form       VARCHAR(32)     NULL,
    unit_price        DECIMAL(10, 2)  NOT NULL,
    prescription_type VARCHAR(8)      NOT NULL DEFAULT 'RX',
    status            VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_drug_code (drug_code),
    KEY idx_drug_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品目录';

CREATE TABLE drug_batch (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    drug_id         BIGINT UNSIGNED NOT NULL,
    batch_no        VARCHAR(64)     NOT NULL,
    production_date DATE            NULL,
    expiry_date     DATE            NOT NULL,
    stock_qty       INT             NOT NULL,
    reserved_qty    INT             NOT NULL DEFAULT 0,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    version         INT             NOT NULL DEFAULT 0,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_drug_no (drug_id, batch_no),
    KEY idx_batch_fefo (drug_id, status, expiry_date, id),
    CONSTRAINT fk_batch_drug FOREIGN KEY (drug_id) REFERENCES drug (id),
    CONSTRAINT chk_batch_qty CHECK (stock_qty >= 0 AND reserved_qty >= 0 AND reserved_qty <= stock_qty)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品批次';

CREATE TABLE prescription (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_no  VARCHAR(32)     NOT NULL,
    encounter_id     BIGINT UNSIGNED NOT NULL,
    patient_id       BIGINT UNSIGNED NOT NULL,
    dept_id          BIGINT UNSIGNED NOT NULL,
    doctor_id        BIGINT UNSIGNED NOT NULL,
    type             VARCHAR(16)     NOT NULL DEFAULT 'WESTERN',
    status           VARCHAR(32)     NOT NULL DEFAULT 'DRAFT',
    revision_no      INT             NOT NULL DEFAULT 0,
    diagnosis_summary VARCHAR(500)   NULL,
    medical_advice   VARCHAR(500)    NULL,
    submitted_at     DATETIME(3)     NULL,
    reviewed_by      BIGINT UNSIGNED NULL,
    reviewed_at      DATETIME(3)     NULL,
    reject_reason    VARCHAR(255)    NULL,
    dispensed_by     BIGINT UNSIGNED NULL,
    dispensed_at     DATETIME(3)     NULL,
    cancel_reason    VARCHAR(255)    NULL,
    expires_at       DATETIME(3)     NULL,
    created_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by       BIGINT UNSIGNED NULL,
    updated_at       DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by       BIGINT UNSIGNED NULL,
    version          INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prescription_no (prescription_no),
    KEY idx_rx_status_dept (status, dept_id, created_at),
    KEY idx_rx_patient (patient_id, created_at),
    KEY idx_rx_encounter (encounter_id),
    KEY idx_rx_expire (status, expires_at),
    CONSTRAINT fk_rx_encounter FOREIGN KEY (encounter_id) REFERENCES encounter (id),
    CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_rx_doctor FOREIGN KEY (doctor_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方';

CREATE TABLE prescription_item (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT UNSIGNED NOT NULL,
    drug_id         BIGINT UNSIGNED NOT NULL,
    drug_name       VARCHAR(100)    NOT NULL,
    spec            VARCHAR(100)    NOT NULL,
    dosage          VARCHAR(64)     NOT NULL,
    frequency       VARCHAR(64)     NOT NULL,
    route           VARCHAR(32)     NOT NULL,
    days            INT             NOT NULL,
    quantity        INT             NOT NULL,
    unit            VARCHAR(16)     NOT NULL,
    unit_price      DECIMAL(10, 2)  NOT NULL,
    amount          DECIMAL(10, 2)  NOT NULL,
    note            VARCHAR(255)    NULL,
    sort_no         INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_rx_item_rx (prescription_id),
    CONSTRAINT fk_rx_item_rx FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_rx_item_drug FOREIGN KEY (drug_id) REFERENCES drug (id),
    CONSTRAINT chk_rx_item_qty CHECK (quantity > 0 AND days > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方明细';

CREATE TABLE prescription_revision (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT UNSIGNED NOT NULL,
    revision_no    INT             NOT NULL,
    content_json   JSON            NOT NULL,
    content_hash   CHAR(64)        NOT NULL,
    submitted_by   BIGINT UNSIGNED NOT NULL,
    submitted_at   DATETIME(3)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rx_revision (prescription_id, revision_no),
    CONSTRAINT fk_rx_rev_rx FOREIGN KEY (prescription_id) REFERENCES prescription (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方修订快照';

CREATE TABLE prescription_review (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT UNSIGNED NOT NULL,
    revision_no     INT             NOT NULL,
    reviewer_id     BIGINT UNSIGNED NOT NULL,
    decision        VARCHAR(16)     NOT NULL,
    reason          VARCHAR(255)    NULL,
    reviewed_at     DATETIME(3)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_review_rx (prescription_id, reviewed_at),
    CONSTRAINT fk_rx_review_rx FOREIGN KEY (prescription_id) REFERENCES prescription (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审方记录';

CREATE TABLE prescription_event (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT UNSIGNED NOT NULL,
    action          VARCHAR(32)     NOT NULL,
    from_status     VARCHAR(32)     NULL,
    to_status       VARCHAR(32)     NULL,
    actor_id        BIGINT UNSIGNED NULL,
    reason          VARCHAR(255)    NULL,
    payload         JSON            NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_rx_event (prescription_id, created_at),
    CONSTRAINT fk_rx_event_rx FOREIGN KEY (prescription_id) REFERENCES prescription (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处方事件';

CREATE TABLE stock_reservation (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT UNSIGNED NOT NULL,
    prescription_item_id BIGINT UNSIGNED NOT NULL,
    drug_id         BIGINT UNSIGNED NOT NULL,
    batch_id        BIGINT UNSIGNED NOT NULL,
    quantity        INT             NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'HELD',
    idem_key        VARCHAR(128)    NOT NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_idem (idem_key),
    KEY idx_reservation_rx (prescription_id, status),
    KEY idx_reservation_batch (batch_id, status),
    CONSTRAINT fk_res_rx FOREIGN KEY (prescription_id) REFERENCES prescription (id),
    CONSTRAINT fk_res_batch FOREIGN KEY (batch_id) REFERENCES drug_batch (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存预占';

CREATE TABLE stock_movement (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    movement_no  VARCHAR(32)     NOT NULL,
    drug_id      BIGINT UNSIGNED NOT NULL,
    batch_id     BIGINT UNSIGNED NOT NULL,
    type         VARCHAR(16)     NOT NULL,
    quantity     INT             NOT NULL,
    before_qty   INT             NOT NULL,
    after_qty    INT             NOT NULL,
    biz_type     VARCHAR(32)     NOT NULL,
    biz_id       BIGINT UNSIGNED NOT NULL,
    idem_key     VARCHAR(128)    NOT NULL,
    operator_id  BIGINT UNSIGNED NULL,
    remark       VARCHAR(255)    NULL,
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_movement_no (movement_no),
    UNIQUE KEY uk_movement_idem (idem_key),
    KEY idx_movement_batch (batch_id, created_at),
    KEY idx_movement_biz (biz_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存流水';

INSERT INTO drug (id, drug_code, name, spec, unit, dosage_form, unit_price, prescription_type) VALUES
    (1, 'AMOX500', '阿莫西林胶囊', '0.5g*24粒', '盒', '胶囊', 18.50, 'RX'),
    (2, 'IBU400', '布洛芬片', '0.4g*20片', '盒', '片剂', 12.00, 'OTC'),
    (3, 'SALB100', '沙丁胺醇气雾剂', '100μg*200揿', '支', '气雾剂', 35.00, 'RX');

INSERT INTO drug_batch (id, drug_id, batch_no, production_date, expiry_date, stock_qty, reserved_qty, status) VALUES
    (1, 1, 'AMOX-2026A', '2026-01-01', '2027-06-30', 100, 0, 'ACTIVE'),
    (2, 1, 'AMOX-2025B', '2025-01-01', '2026-12-31', 30, 0, 'ACTIVE'),
    (3, 2, 'IBU-2026A', '2026-02-01', '2027-12-31', 80, 0, 'ACTIVE'),
    (4, 3, 'SALB-2026A', '2026-03-01', '2027-09-30', 40, 0, 'ACTIVE');

INSERT INTO sys_permission (id, code, name, type, route_path, icon, sort_no) VALUES
    (53, 'menu:pharmacy', '药房审方', 'MENU', '/pharmacy', 'pill', 45);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES (4, 53);
