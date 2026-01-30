-- =====================================================
-- 添加统一服务发起菜单
-- 放置在「服务项发起」目录下
-- =====================================================

-- 获取「服务项发起」目录的ID
SET @service_initiate_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '服务项发起' AND `deleted` = b'0' LIMIT 1);

-- 在「服务项发起」目录下添加「统一服务发起」菜单（排序在第一位）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '服务发起', '', 2, 0, @service_initiate_menu_id, 'service-launch', 'ep:promotion', 'project/service-launch/index', 'ServiceLaunchList', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_initiate_menu_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @service_initiate_menu_id AND path = 'service-launch' AND deleted = b'0'
);

SET @service_launch_menu_id = (SELECT id FROM system_menu WHERE parent_id = @service_initiate_menu_id AND path = 'service-launch' AND deleted = b'0' LIMIT 1);

-- 添加统一服务发起的权限按钮
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '发起服务', 'project:service-launch:create', 3, 1, @service_launch_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_launch_menu_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @service_launch_menu_id AND permission = 'project:service-launch:create' AND deleted = b'0');

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '查看服务发起', 'project:service-launch:query', 3, 2, @service_launch_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_launch_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @service_launch_menu_id AND permission = 'project:service-launch:query' AND deleted = b'0');

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '删除服务发起', 'project:service-launch:delete', 3, 3, @service_launch_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_launch_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @service_launch_menu_id AND permission = 'project:service-launch:delete' AND deleted = b'0');

-- 为超级管理员角色分配菜单权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @service_launch_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @service_launch_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @service_launch_menu_id AND deleted = b'0');

-- 为超级管理员角色分配按钮权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM system_menu m
WHERE m.parent_id = @service_launch_menu_id 
AND m.type = 3 
AND m.deleted = b'0'
AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = m.id AND deleted = b'0');
