-- 修复安全运营详情页菜单配置
-- 问题：详情页作为安全运营的子菜单，导致点击安全运营时自动跳转到详情页
-- 解决：把详情页改为项目管理的直接子菜单，路径改为 security-operation/detail/:id

-- 1. 获取项目管理菜单ID
SET @project_menu_id = (SELECT id FROM system_menu WHERE name = '项目管理' AND parent_id = 0 AND deleted = 0 LIMIT 1);

-- 2. 删除旧的安全运营详情菜单
DELETE FROM system_menu WHERE name = '安全运营详情' AND deleted = 0;

-- 3. 重新创建安全运营详情菜单，作为项目管理的子菜单
-- path 改为完整路径：security-operation/detail/:id（相对于 /project）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('安全运营详情', '', 2, 99, @project_menu_id, 'security-operation/detail/:id', '', 'project/security-operation/detail', 'SecurityOperationDetail', 0, 0, 1, 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 验证：查看安全运营相关菜单
-- SELECT id, name, parent_id, path, component, visible, type FROM system_menu WHERE name LIKE '%安全运营%' AND deleted = 0 ORDER BY id;
