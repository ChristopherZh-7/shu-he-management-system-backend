-- 经营分析菜单
-- 放在成本管理下面

-- 1. 查找成本管理菜单的ID
SET @cost_parent_id = (SELECT id FROM system_menu WHERE name = '成本管理' AND deleted = 0 LIMIT 1);

-- 2. 如果成本管理菜单不存在，则使用根目录
SET @parent_id = COALESCE(@cost_parent_id, 0);

-- 3. 插入经营分析菜单
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`, `status`,
    `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '经营分析', '', 2, 60, @parent_id,
    'business-analysis', 'ep:data-analysis', 'cost-management/business-analysis/index', 'BusinessAnalysis', 0,
    1, 1, 1,
    '1', NOW(), '1', NOW(), 0
);

-- 4. 获取刚插入的菜单ID
SET @menu_id = LAST_INSERT_ID();

-- 5. 插入查询权限按钮
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`, `status`,
    `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '经营分析查询', 'system:business-analysis:query', 3, 1, @menu_id,
    '', '', '', '', 0,
    1, 1, 1,
    '1', NOW(), '1', NOW(), 0
);

-- 6. 给超级管理员角色分配权限
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, @menu_id, '1', NOW(), '1', NOW(), 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @menu_id AND deleted = 0
);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, LAST_INSERT_ID(), '1', NOW(), '1', NOW(), 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = LAST_INSERT_ID() AND deleted = 0
);
