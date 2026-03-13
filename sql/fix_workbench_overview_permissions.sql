-- =============================================
-- 修复「工作台 - 团队工作总览 / 全局总览」403 权限问题
--
-- 原因：团队工作总览、全局总览页面会调用多个接口，需要以下权限：
--   - project:team-overview:query   (团队工作总览 API)
--   - project:global-overview:query (全局总览 API)
--   - project:daily-record:query    (当前周信息 current-week-info 等)
--   - project:my-work-record:query (获取可选项目列表 my-projects，团队总览下部 Tab2 用)
--
-- 总经办等角色可能只有工作台菜单，但缺少上述按钮权限，导致 403。
--
-- 本脚本：为拥有「工作台」「团队工作总览」「全局总览」任一菜单的角色，
--        以及角色名为「总经办」的角色，补充上述 3 个查询权限。
-- =============================================

SET NAMES utf8mb4;

-- 工作台、团队工作总览、全局总览菜单 ID
SET @workbench_id = (SELECT id FROM system_menu WHERE (path = 'workbench' OR name = '工作台') AND deleted = 0 LIMIT 1);
SET @team_overview_id = (SELECT id FROM system_menu WHERE path = 'team-overview' AND deleted = 0 LIMIT 1);
SET @global_overview_id = (SELECT id FROM system_menu WHERE path = 'global-overview' AND deleted = 0 LIMIT 1);

-- 4 个必需的权限菜单 ID
SET @team_overview_query_id = (SELECT id FROM system_menu WHERE permission = 'project:team-overview:query' AND deleted = 0 LIMIT 1);
SET @global_overview_query_id = (SELECT id FROM system_menu WHERE permission = 'project:global-overview:query' AND deleted = 0 LIMIT 1);
SET @daily_record_query_id = (SELECT id FROM system_menu WHERE permission = 'project:daily-record:query' AND deleted = 0 LIMIT 1);
SET @my_work_record_query_id = (SELECT id FROM system_menu WHERE permission = 'project:my-work-record:query' AND deleted = 0 LIMIT 1);

-- 目标角色：拥有工作台/团队总览/全局总览任一菜单，或角色名为「总经办」
-- 为这些角色补充 3 个查询权限

-- project:team-overview:query
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @team_overview_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r
WHERE r.deleted = 0
  AND @team_overview_query_id IS NOT NULL
  AND (
    r.name = '总经办'
    OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id))
  )
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @team_overview_query_id AND rm2.deleted = 0);

-- project:global-overview:query
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @global_overview_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r
WHERE r.deleted = 0
  AND @global_overview_query_id IS NOT NULL
  AND (
    r.name = '总经办'
    OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id))
  )
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @global_overview_query_id AND rm2.deleted = 0);

-- project:daily-record:query（用于 current-week-info 等接口）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @daily_record_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r
WHERE r.deleted = 0
  AND @daily_record_query_id IS NOT NULL
  AND (
    r.name = '总经办'
    OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id))
  )
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @daily_record_query_id AND rm2.deleted = 0);

-- project:my-work-record:query（用于 my-projects 接口，团队总览下部项目反馈 Tab）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, @my_work_record_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r
WHERE r.deleted = 0
  AND @my_work_record_query_id IS NOT NULL
  AND (
    r.name = '总经办'
    OR EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.deleted = 0 AND rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id))
  )
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = r.id AND rm2.menu_id = @my_work_record_query_id AND rm2.deleted = 0);

SELECT 'fix_workbench_overview_permissions done. 请重新登录后生效。' AS result;
