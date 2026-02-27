-- =============================================
-- 新增全局总览菜单（总经办/总经理专用）
-- =============================================

SET NAMES utf8mb4;

-- 获取工作台菜单ID
SET @workbench_id = (
    SELECT `id` FROM `system_menu`
    WHERE (`path` = 'workbench' OR `name` = '工作台')
      AND `deleted` = 0
    LIMIT 1
);

-- 创建全局总览菜单
SET @global_overview_id = (
    SELECT `id` FROM `system_menu`
    WHERE `path` = 'global-overview' AND `deleted` = 0
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`,
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '全局总览', '', 2, 3, @workbench_id, 'global-overview',
       'ep:data-analysis', 'dashboard/global-overview/index', 'WorkbenchGlobalOverview',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @global_overview_id IS NULL AND @workbench_id IS NOT NULL;

SET @global_overview_id = COALESCE(
    @global_overview_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'global-overview' AND `deleted` = 0 LIMIT 1)
);

-- 权限按钮
SET @go_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:global-overview:query' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '全局总览查询', 'project:global-overview:query', 3, 1, @global_overview_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @go_query_id IS NULL AND @global_overview_id IS NOT NULL;

SET @go_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:global-overview:query' AND `deleted` = 0 LIMIT 1);

-- 超级管理员
SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` IN (@global_overview_id, @go_query_id) AND `deleted` = 0;

-- 主管角色也分配（他们也可能需要看全局）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@global_overview_id, @go_query_id);

-- 验证
SELECT '===== 全局总览菜单 =====' AS title;
SELECT id, name, path, component_name, parent_id FROM `system_menu`
WHERE `path` = 'global-overview' AND `deleted` = 0;

SELECT '✅ 全局总览菜单创建完成！' AS result;
