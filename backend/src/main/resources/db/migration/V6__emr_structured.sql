-- MedFlow V6: structured EMR templates, versions, signing and amendments.

CREATE TABLE emr_template (
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code               VARCHAR(64)     NOT NULL,
    name               VARCHAR(100)    NOT NULL,
    scope_type         VARCHAR(16)     NOT NULL,
    dept_id            BIGINT UNSIGNED NULL,
    status             VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
    current_version_id BIGINT UNSIGNED NULL,
    created_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by         BIGINT UNSIGNED NULL,
    updated_at         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by         BIGINT UNSIGNED NULL,
    version            INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_emr_template_code (code),
    KEY idx_emr_template_scope (scope_type, dept_id, status),
    CONSTRAINT fk_emr_template_dept FOREIGN KEY (dept_id) REFERENCES sys_department (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病历模板';

CREATE TABLE emr_template_version (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_id  BIGINT UNSIGNED NOT NULL,
    version_no   INT             NOT NULL,
    schema_json  JSON            NOT NULL,
    changelog    VARCHAR(255)    NULL,
    status       VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED',
    published_by BIGINT UNSIGNED NULL,
    published_at DATETIME(3)     NULL,
    created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_emr_template_version (template_id, version_no),
    CONSTRAINT fk_emr_tv_template FOREIGN KEY (template_id) REFERENCES emr_template (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病历模板版本（发布后不可改）';

INSERT INTO emr_template (id, code, name, scope_type, dept_id, status)
VALUES (1, 'GENERAL_FIRST', '全科门诊病历', 'HOSPITAL', NULL, 'PUBLISHED');

INSERT INTO emr_template_version
    (id, template_id, version_no, schema_json, changelog, status, published_by, published_at)
VALUES (1, 1, 1,
        '{"sections":[{"key":"chief","title":"主诉与现病史","fields":[{"key":"chiefComplaint","type":"textarea","label":"主诉","required":true,"maxLength":200},{"key":"fever","type":"boolean","label":"发热"},{"key":"temperature","type":"number","label":"体温","unit":"℃","min":34,"max":43,"visibleWhen":{"field":"fever","op":"eq","value":true},"requiredWhen":{"field":"fever","op":"eq","value":true}},{"key":"onsetDate","type":"date","label":"发病日期"},{"key":"presentIllness","type":"textarea","label":"现病史","maxLength":1000}]},{"key":"history","title":"既往与查体","fields":[{"key":"pastHistory","type":"textarea","label":"既往史","maxLength":1000},{"key":"physicalExam","type":"textarea","label":"体格检查","maxLength":1000}]},{"key":"plan","title":"辅助检查与处理","fields":[{"key":"auxiliaryExam","type":"textarea","label":"辅助检查","maxLength":1000},{"key":"treatmentPlan","type":"textarea","label":"处理意见","maxLength":1000}]}]}',
        '内置全科初诊模板', 'PUBLISHED', 1, NOW(3));

UPDATE emr_template SET current_version_id = 1 WHERE id = 1;

ALTER TABLE emr_record
    ADD COLUMN template_id BIGINT UNSIGNED NULL AFTER doctor_id,
    ADD COLUMN template_version_id BIGINT UNSIGNED NULL AFTER template_id,
    ADD COLUMN signed_by BIGINT UNSIGNED NULL AFTER signed_at,
    ADD COLUMN signature_hash CHAR(64) NULL AFTER signed_by,
    ADD COLUMN amended_at DATETIME(3) NULL AFTER signature_hash,
    ADD COLUMN amend_reason VARCHAR(255) NULL AFTER amended_at;

UPDATE emr_record SET template_id = 1, template_version_id = 1
WHERE template_id IS NULL OR template_version_id IS NULL;

ALTER TABLE emr_record
    MODIFY template_id BIGINT UNSIGNED NOT NULL,
    MODIFY template_version_id BIGINT UNSIGNED NOT NULL,
    ADD CONSTRAINT fk_emr_template FOREIGN KEY (template_id) REFERENCES emr_template (id),
    ADD CONSTRAINT fk_emr_template_version FOREIGN KEY (template_version_id) REFERENCES emr_template_version (id);

CREATE TABLE emr_record_version (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    record_id      BIGINT UNSIGNED NOT NULL,
    version_no     INT             NOT NULL,
    content_json   JSON            NOT NULL,
    content_hash   CHAR(64)        NULL,
    status         VARCHAR(16)     NOT NULL,
    author_id      BIGINT UNSIGNED NULL,
    signed_by      BIGINT UNSIGNED NULL,
    signed_at      DATETIME(3)     NULL,
    change_reason  VARCHAR(255)    NULL,
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_emr_version (record_id, version_no),
    KEY idx_emr_version_signed (record_id, signed_at),
    CONSTRAINT fk_emrv_record FOREIGN KEY (record_id) REFERENCES emr_record (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '病历签署快照';

INSERT INTO sys_permission (id, code, name, type, route_path, icon, sort_no) VALUES
    (52, 'menu:emr-templates', '病历模板', 'MENU', '/admin/emr-templates', 'file-text', 35);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES (1, 52);
