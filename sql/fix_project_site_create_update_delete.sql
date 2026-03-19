-- =============================================
-- 修复 project/site/create、update、delete 返回 403
--
-- 原因：这三个接口需要 project:site:create/update/delete 权限，
--       但三个模块下可能缺少这些菜单项
--
-- 本脚本：在三个模块下添加 project:site:create/update/delete，
--        并分配给已拥有对应父菜单的角色
-- =============================================

SET NAMES utf8mb4;

SET @security_service_id = (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0 LIMIT 1);
SET @security_operation_id = (SELECT id FROM system_menu WHERE path = 'security-operation' AND deleted = 0 LIMIT 1);
SET @data_security_id = (SELECT id FROM system_menu WHERE path = 'data-security' AND deleted = 0 LIMIT 1);

-- 为每个模块添加 project:site:create, update, delete
-- 安全服务
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:create' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:update' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:delete' AND parent_id = @security_service_id AND deleted = 0);

-- 安全运营
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:create' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:update' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:delete' AND parent_id = @security_operation_id AND deleted = 0);

-- 数据安全
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:create' AND parent_id = @data_security_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:update' AND parent_id = @data_security_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:delete' AND parent_id = @data_security_id AND deleted = 0);

-- 为已拥有父菜单的角色分配
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_service_id AND m.deleted = 0
  AND m.permission IN ('project:site:create','project:site:update','project:site:delete')
WHERE rm.menu_id = @security_service_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_operation_id AND m.deleted = 0
  AND m.permission IN ('project:site:create','project:site:update','project:site:delete')
WHERE rm.menu_id = @security_operation_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @data_security_id AND m.deleted = 0
  AND m.permission IN ('project:site:create','project:site:update','project:site:delete')
WHERE rm.menu_id = @data_security_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'Done. Clear Redis cache and re-login.' AS result;
