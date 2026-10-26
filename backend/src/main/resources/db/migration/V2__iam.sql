-- MedFlow V2: IAM (departments, users, roles, permissions) + demo seed data.
-- Demo accounts share the password: Medflow@2026 (BCrypt cost 12).

CREATE TABLE sys_department (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code       VARCHAR(32)     NOT NULL,
    name       VARCHAR(64)     NOT NULL,
    parent_id  BIGINT UNSIGNED NULL,
    type       VARCHAR(16)     NOT NULL COMMENT 'CLINICAL / PHARMACY / ADMIN',
    status     VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    sort_no    INT             NOT NULL DEFAULT 0,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NULL,
    updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NULL,
    version    INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code),
    KEY idx_department_parent (parent_id),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES sys_department (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '科室';

CREATE TABLE sys_role (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code       VARCHAR(32)     NOT NULL,
    name       VARCHAR(64)     NOT NULL,
    data_scope VARCHAR(16)     NOT NULL COMMENT 'ALL / DEPT / SELF',
    built_in   TINYINT         NOT NULL DEFAULT 0,
    status     VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    remark     VARCHAR(255)    NULL,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NULL,
    updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by BIGINT UNSIGNED NULL,
    version    INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色';

CREATE TABLE sys_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)     NOT NULL,
    name        VARCHAR(64)     NOT NULL,
    type        VARCHAR(16)     NOT NULL COMMENT 'MENU / BUTTON / API',
    parent_id   BIGINT UNSIGNED NULL,
    route_path  VARCHAR(128)    NULL,
    component   VARCHAR(128)    NULL,
    icon        VARCHAR(32)     NULL,
    http_method VARCHAR(10)     NULL,
    api_pattern VARCHAR(128)    NULL,
    sort_no     INT             NOT NULL DEFAULT 0,
    status      VARCHAR(16)     NOT NULL DEFAULT 'ENABLED',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by  BIGINT UNSIGNED NULL,
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by  BIGINT UNSIGNED NULL,
    version     INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code),
    KEY idx_permission_parent (parent_id),
    CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限（菜单/按钮/接口）';

CREATE TABLE sys_user (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username            VARCHAR(50)     NOT NULL,
    password_hash       VARCHAR(100)    NOT NULL,
    real_name           VARCHAR(50)     NOT NULL,
    employee_no         VARCHAR(32)     NULL,
    dept_id             BIGINT UNSIGNED NOT NULL,
    title               VARCHAR(32)     NULL,
    phone               VARCHAR(20)     NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED / LOCKED',
    failed_attempts     INT             NOT NULL DEFAULT 0,
    locked_until        DATETIME(3)     NULL,
    last_login_at       DATETIME(3)     NULL,
    password_changed_at DATETIME(3)     NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          BIGINT UNSIGNED NULL,
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    updated_by          BIGINT UNSIGNED NULL,
    version             INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_employee_no (employee_no),
    KEY idx_user_dept_status (dept_id, status),
    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_department (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';

CREATE TABLE sys_user_role (
    user_id    BIGINT UNSIGNED NOT NULL,
    role_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色';

CREATE TABLE sys_role_permission (
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permission_permission (permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限';

-- ---------------------------------------------------------------------------
-- Demo seed
-- ---------------------------------------------------------------------------

INSERT INTO sys_department (id, code, name, parent_id, type, sort_no) VALUES
    (1, 'DERM', '皮肤科', NULL, 'CLINICAL', 10),
    (2, 'GEN', '全科医学科', NULL, 'CLINICAL', 20),
    (3, 'PHARM', '药房', NULL, 'PHARMACY', 30),
    (4, 'ADMIN', '行政管理', NULL, 'ADMIN', 90);

INSERT INTO sys_role (id, code, name, data_scope, built_in, remark) VALUES
    (1, 'ADMIN', '系统管理员', 'ALL', 1, '用户、角色、科室、审计与模板管理'),
    (2, 'DOCTOR', '医生', 'DEPT', 1, '接诊、病历与处方'),
    (3, 'NURSE', '护士', 'DEPT', 1, '建档、签到与候诊'),
    (4, 'PHARMACIST', '药师', 'ALL', 1, '审方、发药与库存'),
    (5, 'CASHIER', '收费员', 'DEPT', 1, '挂号收费与退费');

INSERT INTO sys_permission (id, code, name, type, parent_id, route_path, icon, sort_no) VALUES
    (1, 'menu:dashboard', '工作台', 'MENU', NULL, '/dashboard', 'dashboard', 10),
    (2, 'menu:staff', '科室人员', 'MENU', NULL, '/staff', 'users', 20),
    (3, 'menu:system', '系统管理', 'MENU', NULL, NULL, 'lock', 90),
    (4, 'system:user:read', '用户管理', 'MENU', 3, '/admin/users', 'users', 91),
    (5, 'system:role:read', '角色权限', 'MENU', 3, '/admin/roles', 'lock', 92),
    (6, 'system:dept:read', '科室管理', 'MENU', 3, '/admin/departments', 'clipboard', 93),
    (7, 'system:audit:read', '审计日志', 'MENU', 3, '/admin/audit', 'shield', 94);

INSERT INTO sys_permission (id, code, name, type, parent_id, http_method, api_pattern, sort_no) VALUES
    (8, 'system:user:create', '新建用户', 'BUTTON', 4, 'POST', '/api/v1/users', 1),
    (9, 'system:user:update', '编辑用户', 'BUTTON', 4, 'PUT', '/api/v1/users/*', 2),
    (10, 'system:user:reset-password', '重置密码', 'BUTTON', 4, 'POST', '/api/v1/users/*/reset-password', 3),
    (11, 'system:user:disable', '启用/停用用户', 'BUTTON', 4, 'PATCH', '/api/v1/users/*/status', 4),
    (12, 'system:role:create', '新建角色', 'BUTTON', 5, 'POST', '/api/v1/roles', 1),
    (13, 'system:role:update', '编辑角色', 'BUTTON', 5, 'PUT', '/api/v1/roles/*', 2),
    (14, 'system:role:assign-permission', '角色授权', 'BUTTON', 5, 'PUT', '/api/v1/roles/*/permissions', 3),
    (15, 'system:dept:manage', '维护科室', 'BUTTON', 6, 'POST', '/api/v1/departments', 1);

INSERT INTO sys_permission (id, code, name, type, http_method, api_pattern, sort_no) VALUES
    (16, 'patient:read', '查看患者', 'API', 'GET', '/api/v1/patients/**', 10),
    (17, 'patient:create', '患者建档', 'API', 'POST', '/api/v1/patients', 11),
    (18, 'patient:update', '修改患者', 'API', 'PUT', '/api/v1/patients/*', 12),
    (19, 'schedule:read', '查看排班', 'API', 'GET', '/api/v1/schedules/**', 20),
    (20, 'schedule:manage', '维护排班', 'API', 'POST', '/api/v1/schedules/**', 21),
    (21, 'appointment:read', '查看预约', 'API', 'GET', '/api/v1/appointments/**', 30),
    (22, 'appointment:create', '预约挂号', 'API', 'POST', '/api/v1/appointments', 31),
    (23, 'appointment:cancel', '取消预约', 'API', 'POST', '/api/v1/appointments/*/cancel', 32),
    (24, 'appointment:checkin', '签到', 'API', 'POST', '/api/v1/appointments/*/check-in', 33),
    (25, 'charge:read', '查看收费', 'API', 'GET', '/api/v1/charges/**', 40),
    (26, 'charge:collect', '收费', 'API', 'POST', '/api/v1/charges/*/pay', 41),
    (27, 'charge:refund', '退费', 'API', 'POST', '/api/v1/charges/*/refund', 42),
    (28, 'encounter:read', '查看就诊', 'API', 'GET', '/api/v1/encounters/**', 50),
    (29, 'encounter:start', '开始接诊', 'API', 'POST', '/api/v1/encounters/*/start', 51),
    (30, 'encounter:finish', '结束就诊', 'API', 'POST', '/api/v1/encounters/*/finish', 52),
    (31, 'emr:read', '查看病历', 'API', 'GET', '/api/v1/emr/**', 60),
    (32, 'emr:edit', '编辑病历', 'API', 'PUT', '/api/v1/emr/**', 61),
    (33, 'emr:sign', '签署病历', 'API', 'POST', '/api/v1/emr/*/sign', 62),
    (34, 'emr:revise', '修订病历', 'API', 'POST', '/api/v1/emr/*/revise', 63),
    (35, 'emr:template:read', '查看病历模板', 'API', 'GET', '/api/v1/emr-templates/**', 64),
    (36, 'emr:template:manage', '维护病历模板', 'API', 'POST', '/api/v1/emr-templates/**', 65),
    (37, 'prescription:read', '查看处方', 'API', 'GET', '/api/v1/prescriptions/**', 70),
    (38, 'prescription:create', '创建处方', 'API', 'POST', '/api/v1/prescriptions', 71),
    (39, 'prescription:submit', '提交处方', 'API', 'POST', '/api/v1/prescriptions/*/submit', 72),
    (40, 'prescription:revise', '修订处方', 'API', 'PUT', '/api/v1/prescriptions/*', 73),
    (41, 'prescription:cancel', '取消处方', 'API', 'POST', '/api/v1/prescriptions/*/cancel', 74),
    (42, 'pharmacy:review', '审方', 'API', 'POST', '/api/v1/pharmacy/reviews/**', 80),
    (43, 'pharmacy:dispense', '发药', 'API', 'POST', '/api/v1/pharmacy/dispense/**', 81),
    (44, 'pharmacy:return', '退药', 'API', 'POST', '/api/v1/pharmacy/returns/**', 82),
    (45, 'drug:read', '查看药品', 'API', 'GET', '/api/v1/drugs/**', 90),
    (46, 'drug:manage', '维护药品', 'API', 'POST', '/api/v1/drugs/**', 91),
    (47, 'stock:read', '查看库存', 'API', 'GET', '/api/v1/stock/**', 92),
    (48, 'stock:manage', '维护库存', 'API', 'POST', '/api/v1/stock/**', 93);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (1, 1), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
    (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 36),
    (2, 1), (2, 2), (2, 6), (2, 16), (2, 18), (2, 19), (2, 20), (2, 21),
    (2, 28), (2, 29), (2, 30), (2, 31), (2, 32), (2, 33), (2, 34), (2, 35),
    (2, 37), (2, 38), (2, 39), (2, 40), (2, 41), (2, 45),
    (3, 1), (3, 2), (3, 6), (3, 16), (3, 17), (3, 18), (3, 19), (3, 21), (3, 24), (3, 28), (3, 31),
    (4, 1), (4, 6), (4, 16), (4, 37), (4, 42), (4, 43), (4, 44), (4, 45), (4, 46), (4, 47), (4, 48),
    (5, 1), (5, 2), (5, 6), (5, 16), (5, 17), (5, 18), (5, 19), (5, 21), (5, 22), (5, 23),
    (5, 25), (5, 26), (5, 27);

INSERT INTO sys_user
    (id, username, password_hash, real_name, employee_no, dept_id, title, phone, status)
VALUES
    (1, 'admin', '$2y$12$WRUjX7E94bUzjFu.0e6tVOd439DC0JaBDd2MWktAvKfiXqk7v2epi',
     '系统管理员', 'A0001', 4, '信息科', '13800000001', 'ENABLED'),
    (2, 'doctor1', '$2y$12$gAZVQ2CV5IJ5L2SsuEEeuOrf2aKT9KVlWcSVcO6SbVAly60bSiZ4q',
     '张明', 'D0001', 1, '主任医师', '13800000002', 'ENABLED'),
    (3, 'doctor2', '$2y$12$JXBStRJfjmkcHzo08fQmXOEzfXfc1B/doIewAo9l/kPczB9Yx1avK',
     '李静', 'D0002', 2, '副主任医师', '13800000003', 'ENABLED'),
    (4, 'nurse1', '$2y$12$1x7tZ4nsk9XwUjOJJ4MNEe2eU7NL2Bkw28rbYALXiGv9e67rcXarK',
     '王芳', 'N0001', 1, '护师', '13800000004', 'ENABLED'),
    (5, 'pharmacist1', '$2y$12$qFNYJwqOkqVxdQ3UPOanruU3JbgdcDFxWlWyyA2AnvS58V6N5vr.C',
     '赵磊', 'P0001', 3, '主管药师', '13800000005', 'ENABLED'),
    (6, 'cashier1', '$2y$12$6st3ixSXmjOmbOt2LqdJdeehlOkVcGe7ubopdoeo/b/Y3n7ggFMmC',
     '陈晨', 'C0001', 2, '收费员', '13800000006', 'ENABLED'),
    (7, 'nurse2', '$2y$12$rpOskPTybsE67HzDDMBIee06h8D4i8rF4zBfZawMLS8YRCV6rg1Y6',
     '孙悦', 'N0002', 1, '护士', '13800000007', 'DISABLED');

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1), (2, 2), (3, 2), (4, 3), (5, 4), (6, 5), (7, 3);
