-- =============================================
-- 修复「我的任务」「我的工作记录」404 问题
--
-- 原因：路由由后端菜单动态生成。若角色只有「团队工作总览」「全局总览」等，
--       但没有「我的任务」「我的工作记录」菜单，前端不会注册对应路由，访问即 404。
--
-- 本脚本：为拥有「工作台」「团队工作总览」「全局总览」任一菜单的角色，
--        补充「我的任务」「我的工作记录」及其按钮权限。
-- =============================================

SET NAMES utf8mb4;

SET @workbench_id = (SELECT id FROM system_menu WHERE (path = 'workbench' OR name = '工作台') AND deleted = 0 LIMIT 1);
SET @team_overview_id = (SELECT id FROM system_menu WHERE path = 'team-overview' AND deleted = 0 LIMIT 1);
SET @global_overview_id = (SELECT id FROM system_menu WHERE path = 'global-overview' AND deleted = 0 LIMIT 1);

-- 我的任务、我的工作记录菜单及按钮
SET @my_tasks_id = (SELECT id FROM system_menu WHERE path = 'my-tasks' AND deleted = 0 LIMIT 1);
SET @my_work_record_id = (SELECT id FROM system_menu WHERE path = 'my-work-record' AND deleted = 0 LIMIT 1);
SET @my_tasks_query_id = (SELECT id FROM system_menu WHERE permission = 'project:my-tasks:query' AND deleted = 0 LIMIT 1);
SET @my_work_query_id = (SELECT id FROM system_menu WHERE permission = 'project:my-work-record:query' AND deleted = 0 LIMIT 1);
SET @my_work_create_id = (SELECT id FROM system_menu WHERE permission = 'project:my-work-record:create' AND deleted = 0 LIMIT 1);
SET @my_work_update_id = (SELECT id FROM system_menu WHERE permission = 'project:my-work-record:update' AND deleted = 0 LIMIT 1);

-- 目标：拥有工作台/团队总览/全局总览任一菜单的角色
-- 补充：我的任务菜单 + 我的任务查询、我的工作记录菜单 + 其 3 个按钮

-- 我的任务菜单
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_tasks_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_tasks_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_tasks_id AND rm2.deleted = 0);

-- 我的任务查询
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_tasks_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_tasks_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_tasks_query_id AND rm2.deleted = 0);

-- 我的工作记录菜单
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_work_record_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_work_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_work_record_id AND rm2.deleted = 0);

-- 我的工作记录查询/创建/修改
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_work_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_work_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_work_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_work_create_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_work_create_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_work_create_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @my_work_update_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id IN (@workbench_id, @team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND @my_work_update_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @my_work_update_id AND rm2.deleted = 0);

SELECT 'fix_workbench_my_tasks_my_work_record_404 done. 请清除 Redis 缓存后重新登录。' AS result;
