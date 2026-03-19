-- =============================================
-- 修复用户 226（詹裕文/ay_mg）访问 project/site/list-by-project 返回 403
--
-- 原因：/project/site/list-by-project 需要 project:site:query 权限，
--       ay_mg 角色缺少该权限
--
-- 本脚本：在安全运营、安全服务、数据安全下添加 project:site:query，
--        并分配给已拥有对应父菜单的所有角色
-- =============================================

SET NAMES utf8mb4;

SET @security_service_id = (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0 LIMIT 1);
SET @security_operation_id = (SELECT id FROM system_menu WHERE path = 'security-operation' AND deleted = 0 LIMIT 1);
SET @data_security_id = (SELECT id FROM system_menu WHERE path = 'data-security' AND deleted = 0 LIMIT 1);

-- 1. 安全服务下添加 project:site:query
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:query' AND parent_id = @security_service_id AND deleted = 0);

-- 2. 安全运营下添加 project:site:query
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:query' AND parent_id = @security_operation_id AND deleted = 0);

-- 3. 数据安全下添加 project:site:query
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:site:query' AND parent_id = @data_security_id AND deleted = 0);

-- 4. 为已拥有三个模块菜单的角色分配 project:site:query
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_service_id AND m.permission = 'project:site:query' AND m.deleted = 0
WHERE rm.menu_id = @security_service_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_operation_id AND m.permission = 'project:site:query' AND m.deleted = 0
WHERE rm.menu_id = @security_operation_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @data_security_id AND m.permission = 'project:site:query' AND m.deleted = 0
WHERE rm.menu_id = @data_security_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'Done. Clear Redis cache and re-login.' AS result;
