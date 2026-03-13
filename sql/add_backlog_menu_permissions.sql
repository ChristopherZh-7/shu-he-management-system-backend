-- =============================================
-- 在「待办事项」菜单下添加 5 个查询权限（按钮）
--
-- 原因：待办事项菜单(2701)下原本没有子权限，角色配置时无法勾选。
--       待办页面需要 crm:customer:query、crm:clue:query 等 5 个权限。
--
-- 本脚本：在待办事项下新增 5 个按钮权限，配置角色时展开待办事项即可勾选。
-- =============================================

SET NAMES utf8mb4;

SET @backlog_id = (SELECT id FROM system_menu WHERE path = 'backlog' AND parent_id IN (SELECT id FROM system_menu WHERE path = '/crm' AND deleted = 0) AND deleted = 0 LIMIT 1);

-- 若已存在则跳过
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '待办-客户查询', 'crm:customer:query', 3, 1, @backlog_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @backlog_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @backlog_id AND permission = 'crm:customer:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '待办-线索查询', 'crm:clue:query', 3, 2, @backlog_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @backlog_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @backlog_id AND permission = 'crm:clue:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '待办-合同查询', 'crm:contract:query', 3, 3, @backlog_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @backlog_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @backlog_id AND permission = 'crm:contract:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '待办-回款查询', 'crm:receivable:query', 3, 4, @backlog_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @backlog_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @backlog_id AND permission = 'crm:receivable:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '待办-回款计划查询', 'crm:receivable-plan:query', 3, 5, @backlog_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @backlog_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @backlog_id AND permission = 'crm:receivable-plan:query' AND deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON m.parent_id = @backlog_id AND m.type = 3 AND m.deleted = 0
WHERE rm.menu_id = @backlog_id AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'add_backlog_menu_permissions done. Re-login to take effect.' AS result;
