-- =============================================
-- 统一三个模块（安全服务、安全运营、数据安全）的共用权限项
--
-- 安全运营 补充：project:project:create/update/delete, project:service-item:*
-- 数据安全 补充：project:project:create/update/delete
--
-- 并自动分配给已拥有对应父菜单的角色
-- =============================================

SET NAMES utf8mb4;

SET @security_service_id = (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0 LIMIT 1);
SET @security_operation_id = (SELECT id FROM system_menu WHERE path = 'security-operation' AND deleted = 0 LIMIT 1);
SET @data_security_id = (SELECT id FROM system_menu WHERE path = 'data-security' AND deleted = 0 LIMIT 1);

-- ========== 安全运营：补充 project:project:create/update/delete ==========
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目创建', 'project:project:create', 3, 15, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:create' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目更新', 'project:project:update', 3, 16, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:update' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目删除', 'project:project:delete', 3, 17, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:delete' AND parent_id = @security_operation_id AND deleted = 0);

-- ========== 安全运营：补充 project:service-item:query/create/update/delete ==========
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '服务项查询', 'project:service-item:query', 3, 22, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:service-item:query' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '服务项创建', 'project:service-item:create', 3, 23, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:service-item:create' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '服务项更新', 'project:service-item:update', 3, 24, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:service-item:update' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '服务项删除', 'project:service-item:delete', 3, 25, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:service-item:delete' AND parent_id = @security_operation_id AND deleted = 0);

-- ========== 数据安全：补充 project:project:create/update/delete ==========
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目创建', 'project:project:create', 3, 15, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:create' AND parent_id = @data_security_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目更新', 'project:project:update', 3, 16, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:update' AND parent_id = @data_security_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目删除', 'project:project:delete', 3, 17, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:delete' AND parent_id = @data_security_id AND deleted = 0);

-- ========== 为已拥有父菜单的角色分配新权限 ==========
-- 安全运营
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_operation_id AND m.deleted = 0
  AND m.permission IN ('project:project:create','project:project:update','project:project:delete',
    'project:service-item:query','project:service-item:create','project:service-item:update','project:service-item:delete')
WHERE rm.menu_id = @security_operation_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

-- 数据安全
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @data_security_id AND m.deleted = 0
  AND m.permission IN ('project:project:create','project:project:update','project:project:delete')
WHERE rm.menu_id = @data_security_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'Done. Clear Redis cache and re-login.' AS result;
