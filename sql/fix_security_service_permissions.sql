-- =============================================
-- 修复「安全服务」页面「没有该操作权限」问题
--
-- 原因：后端 API /project/dept-service/page 需要 project:dept-service:query 权限，
--       但菜单中只有 project:info:query，导致 403。
--
-- 本脚本：在「安全服务」「安全运营」「数据安全」下添加 project:dept-service:query 等
--         按钮权限，并分配给已拥有对应菜单的角色。
-- =============================================

SET NAMES utf8mb4;

-- 动态获取菜单 ID（不依赖固定 ID）
SET @security_service_id = (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0
  AND parent_id IN (SELECT id FROM system_menu WHERE path = '/project' AND deleted = 0) LIMIT 1);
SET @security_operation_id = (SELECT id FROM system_menu WHERE path = 'security-operation' AND deleted = 0
  AND parent_id IN (SELECT id FROM system_menu WHERE path = '/project' AND deleted = 0) LIMIT 1);
SET @data_security_id = (SELECT id FROM system_menu WHERE path = 'data-security' AND deleted = 0
  AND parent_id IN (SELECT id FROM system_menu WHERE path = '/project' AND deleted = 0) LIMIT 1);

-- 安全服务：添加 project:dept-service:query（列表查询必需）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:query' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单创建', 'project:dept-service:create', 3, 11, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:create' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单更新', 'project:dept-service:update', 3, 12, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:update' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单删除', 'project:dept-service:delete', 3, 13, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:delete' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目创建', 'project:project:create', 3, 15, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:create' AND parent_id = @security_service_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目更新', 'project:project:update', 3, 16, @security_service_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_service_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:update' AND parent_id = @security_service_id AND deleted = 0);

-- 安全运营、数据安全：同样添加
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:query' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, @data_security_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @data_security_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:query' AND parent_id = @data_security_id AND deleted = 0);

-- 为已拥有「安全服务」菜单的角色分配 project:dept-service:* 等新按钮
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_service_id AND m.type = 3 AND m.deleted = 0
  AND m.permission IN ('project:dept-service:query','project:dept-service:create','project:dept-service:update','project:dept-service:delete','project:project:create','project:project:update')
WHERE rm.menu_id = @security_service_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

-- 安全运营、数据安全同理
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_operation_id AND m.permission = 'project:dept-service:query' AND m.deleted = 0
WHERE rm.menu_id = @security_operation_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @data_security_id AND m.permission = 'project:dept-service:query' AND m.deleted = 0
WHERE rm.menu_id = @data_security_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

-- 超级管理员(role_id=1)确保拥有 project:dept-service:query
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_menu m
WHERE m.permission = 'project:dept-service:query' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.deleted = 0);

SELECT '修复完成！请重新登录后生效。' AS result;
