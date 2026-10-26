-- 科室管理是只读公共页面：从"系统管理"分组移为一级菜单，所有角色可见；
-- 编辑按钮仍由 system:dept:manage 控制（仅 ADMIN）。
UPDATE sys_permission
SET parent_id = NULL, sort_no = 30
WHERE code = 'system:dept:read';
