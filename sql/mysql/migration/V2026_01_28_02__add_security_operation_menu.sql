-- 安全运营菜单配置
-- 重写安全运营模块的菜单结构

-- 先删除旧的安全运营菜单（如果存在）
DELETE FROM system_menu WHERE name = '安全运营' AND parent_id IN (SELECT id FROM (SELECT id FROM system_menu WHERE name = '项目管理') AS t);

-- 获取项目管理菜单ID
SET @project_menu_id = (SELECT id FROM system_menu WHERE name = '项目管理' AND parent_id = 0 LIMIT 1);

-- 创建新的安全运营菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('安全运营', '', 2, 2, @project_menu_id, 'security-operation', 'ep:monitor', 'project/security-operation/index', 'SecurityOperationList', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

SET @so_menu_id = LAST_INSERT_ID();

-- 创建按钮权限
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES 
    ('安全运营查询', 'project:security-operation:query', 3, 1, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营创建', 'project:security-operation:create', 3, 2, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营更新', 'project:security-operation:update', 3, 3, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营删除', 'project:security-operation:delete', 3, 4, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

-- 创建安全运营详情页路由（隐藏菜单）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('安全运营详情', '', 2, 99, @so_menu_id, 'detail/:id', '', 'project/security-operation/detail', 'SecurityOperationDetail', 0, 0, 1, 0, 'admin', NOW(), 'admin', NOW(), 0);
