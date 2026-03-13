-- =============================================
-- 在「团队工作总览」「全局总览」菜单下添加依赖的查询权限（按钮）
--
-- 原因：团队工作总览、全局总览页面会调用 current-week-info、my-projects 等接口，
--       需要 project:daily-record:query、project:my-work-record:query。
--       这些权限原本在「我的工作记录」「日常管理」下，总经办角色配置时
--       只勾选团队/全局总览，看不到也无法勾选这些依赖权限。
--
-- 本脚本：在团队工作总览、全局总览下新增上述 2 个按钮权限，
--        配置角色时展开即可勾选。
-- =============================================

SET NAMES utf8mb4;

SET @team_overview_id = (SELECT id FROM system_menu WHERE path = 'team-overview' AND deleted = 0 LIMIT 1);
SET @global_overview_id = (SELECT id FROM system_menu WHERE path = 'global-overview' AND deleted = 0 LIMIT 1);

-- 团队工作总览下添加：日常记录查询、我的工作记录查询（若尚未存在）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '团队总览-日常记录查询', 'project:daily-record:query', 3, 10, @team_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @team_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @team_overview_id AND permission = 'project:daily-record:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '团队总览-我的工作记录查询', 'project:my-work-record:query', 3, 11, @team_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @team_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @team_overview_id AND permission = 'project:my-work-record:query' AND deleted = 0);

-- 全局总览下添加：日常记录查询、我的工作记录查询（若尚未存在）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '全局总览-日常记录查询', 'project:daily-record:query', 3, 10, @global_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @global_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @global_overview_id AND permission = 'project:daily-record:query' AND deleted = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '全局总览-我的工作记录查询', 'project:my-work-record:query', 3, 11, @global_overview_id, '', '', '', NULL, 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (SELECT 1) x
WHERE @global_overview_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @global_overview_id AND permission = 'project:my-work-record:query' AND deleted = 0);

-- 为已拥有团队总览/全局总览菜单的角色，自动补充这 2 个新按钮权限
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
JOIN system_menu m ON (m.parent_id = @team_overview_id OR m.parent_id = @global_overview_id)
  AND m.type = 3 AND m.permission IN ('project:daily-record:query', 'project:my-work-record:query') AND m.deleted = 0
WHERE rm.menu_id IN (@team_overview_id, @global_overview_id) AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0);

SELECT 'add_workbench_overview_menu_permissions done. 角色配置中可展开团队/全局总览勾选。' AS result;
