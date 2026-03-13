-- =============================================
-- 为「安全运营」补充与「安全服务」相同的部门服务单权限
--
-- 原因：安全服务和安全运营都使用同一个 ProjectList 组件和 API，
--       但安全运营下只有「部门服务单查询」，缺少创建/更新/删除等。
-- =============================================

SET NAMES utf8mb4;

SET @security_operation_id = (SELECT id FROM system_menu WHERE path = 'security-operation' AND deleted = 0
  AND parent_id IN (SELECT id FROM system_menu WHERE path = '/project' AND deleted = 0) LIMIT 1);

-- 安全运营：补充 部门服务单创建/更新/删除、项目创建/更新
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单创建', 'project:dept-service:create', 3, 11, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:create' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单更新', 'project:dept-service:update', 3, 12, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:update' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单删除', 'project:dept-service:delete', 3, 13, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:dept-service:delete' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目创建', 'project:project:create', 3, 15, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:create' AND parent_id = @security_operation_id AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目更新', 'project:project:update', 3, 16, @security_operation_id, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @security_operation_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:update' AND parent_id = @security_operation_id AND deleted = 0);

-- 为已拥有「安全运营」菜单的角色分配新按钮
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @security_operation_id AND m.type = 3 AND m.deleted = 0
  AND m.permission IN ('project:dept-service:create','project:dept-service:update','project:dept-service:delete','project:project:create','project:project:update')
WHERE rm.menu_id = @security_operation_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT '安全运营权限已补全' AS result;
