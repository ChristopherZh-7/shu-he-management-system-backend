-- 修复安全运营菜单配置
-- 彻底删除旧的安全运营相关菜单，重新创建

-- 1. 先查找项目管理菜单ID
SET @project_menu_id = (SELECT id FROM system_menu WHERE name = '项目管理' AND parent_id = 0 AND deleted = 0 LIMIT 1);

-- 2. 查找所有名为"安全运营"的菜单ID（可能有多个）
-- 删除安全运营菜单的所有子菜单
DELETE FROM system_menu WHERE parent_id IN (
    SELECT id FROM (
        SELECT id FROM system_menu WHERE name = '安全运营' AND deleted = 0
    ) AS t
);

-- 3. 删除所有名为"安全运营"的菜单
DELETE FROM system_menu WHERE name = '安全运营' AND deleted = 0;

-- 4. 删除可能残留的安全运营详情菜单
DELETE FROM system_menu WHERE name = '安全运营详情' AND deleted = 0;

-- 5. 删除可能残留的安全运营权限菜单
DELETE FROM system_menu WHERE name IN ('安全运营查询', '安全运营创建', '安全运营更新', '安全运营删除') AND deleted = 0;

-- 6. 重新创建安全运营菜单（列表页）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('安全运营', '', 2, 2, @project_menu_id, 'security-operation', 'ep:monitor', 'project/security-operation/index', 'SecurityOperationList', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

SET @so_menu_id = LAST_INSERT_ID();

-- 7. 创建按钮权限（type=3 按钮）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES 
    ('安全运营查询', 'project:security-operation:query', 3, 1, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营创建', 'project:security-operation:create', 3, 2, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营更新', 'project:security-operation:update', 3, 3, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
    ('安全运营删除', 'project:security-operation:delete', 3, 4, @so_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

-- 8. 创建详情页路由（隐藏菜单，visible=0，用于路由跳转）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('安全运营详情', '', 2, 99, @so_menu_id, 'detail/:id', '', 'project/security-operation/detail', 'SecurityOperationDetail', 0, 0, 1, 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 验证：查看创建的菜单
-- SELECT * FROM system_menu WHERE name LIKE '%安全运营%' AND deleted = 0;
