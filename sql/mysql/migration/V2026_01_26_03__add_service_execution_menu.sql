-- =====================================================
-- 添加服务执行发起菜单（替代原来的渗透测试发起）
-- 放置在「服务项发起」目录下
-- =====================================================

-- 获取「服务项发起」目录的ID
SET @service_initiate_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '服务项发起' AND `deleted` = b'0' LIMIT 1);

-- 在「服务项发起」目录下添加「服务执行发起」菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '服务执行发起', '', 2, 1, @service_initiate_menu_id, 'service-execution', 'ep:video-play', 'project/service-execution/index', 'ServiceExecutionList', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_initiate_menu_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = @service_initiate_menu_id AND path = 'service-execution' AND deleted = b'0'
);

SET @service_execution_menu_id = (SELECT id FROM system_menu WHERE parent_id = @service_initiate_menu_id AND path = 'service-execution' AND deleted = b'0' LIMIT 1);

-- 添加服务执行发起的权限按钮
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '发起服务执行', 'project:service-execution:create', 3, 1, @service_execution_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_execution_menu_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @service_execution_menu_id AND permission = 'project:service-execution:create' AND deleted = b'0');

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '查看服务执行', 'project:service-execution:query', 3, 2, @service_execution_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_execution_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @service_execution_menu_id AND permission = 'project:service-execution:query' AND deleted = b'0');

-- 为超级管理员角色分配权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @service_execution_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @service_execution_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @service_execution_menu_id AND deleted = b'0');
