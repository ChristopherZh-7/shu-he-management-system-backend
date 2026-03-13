-- =============================================
-- 同步工作台总览权限到本地测试环境 (localhost)
-- 在 DataGrip / DBeaver / MySQL Workbench 中打开本文件，选择数据库 shuhe-ms 后执行
-- =============================================

SET NAMES utf8mb4;

-- ========== Part 1: fix_workbench_overview_permissions ==========
SET @workbench_id = (SELECT id FROM system_menu WHERE (path = 'workbench' OR name = '工作台') AND deleted = 0 LIMIT 1);
SET @team_overview_id = (SELECT id FROM system_menu WHERE path = 'team-overview' AND deleted = 0 LIMIT 1);
SET @global_overview_id = (SELECT id FROM system_menu WHERE path = 'global-overview' AND deleted = 0 LIMIT 1);
SET @team_overview_query_id = (SELECT id FROM system_menu WHERE permission = 'project:team-overview:query' AND deleted = 0 LIMIT 1);
SET @global_overview_query_id = (SELECT id FROM system_menu WHERE permission = 'project:global-overview:query' AND deleted = 0 LIMIT 1);
SET @daily_record_query_id = (SELECT id FROM system_menu WHERE permission = 'project:daily-record:query' AND deleted = 0 LIMIT 1);
SET @my_work_record_query_id = (SELECT id FROM system_menu WHERE permission = 'project:my-work-record:query' AND deleted = 0 LIMIT 1);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @team_overview_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r WHERE r.deleted = 0 AND @team_overview_query_id IS NOT NULL
  AND (r.name = '总经办' OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id)))
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @team_overview_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @global_overview_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r WHERE r.deleted = 0 AND @global_overview_query_id IS NOT NULL
  AND (r.name = '总经办' OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id)))
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @global_overview_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @daily_record_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r WHERE r.deleted = 0 AND @daily_record_query_id IS NOT NULL
  AND (r.name = '总经办' OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id)))
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @daily_record_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @my_work_record_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r WHERE r.deleted = 0 AND @my_work_record_query_id IS NOT NULL
  AND (r.name = '总经办' OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id)))
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @my_work_record_query_id AND rm2.deleted = 0);

-- ========== Part 2: add_workbench_overview_menu_permissions ==========
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '团队总览-日常记录查询', 'project:daily-record:query', 3, 10, @team_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x WHERE @team_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @team_overview_id AND permission = 'project:daily-record:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '团队总览-我的工作记录查询', 'project:my-work-record:query', 3, 11, @team_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x WHERE @team_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @team_overview_id AND permission = 'project:my-work-record:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '全局总览-日常记录查询', 'project:daily-record:query', 3, 10, @global_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x WHERE @global_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @global_overview_id AND permission = 'project:daily-record:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '全局总览-我的工作记录查询', 'project:my-work-record:query', 3, 11, @global_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x WHERE @global_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @global_overview_id AND permission = 'project:my-work-record:query' AND deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON (m.parent_id = @team_overview_id OR m.parent_id = @global_overview_id)
  AND m.type = 3 AND m.permission IN ('project:daily-record:query', 'project:my-work-record:query') AND m.deleted = 0
WHERE rm.menu_id IN (@team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'sync_workbench_overview_to_local done. Clear Redis (FLUSHDB) and re-login.' AS result;
