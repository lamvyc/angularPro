-- M4: 管理员可维护模板，也必须能读取模板列表与版本。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 35);
