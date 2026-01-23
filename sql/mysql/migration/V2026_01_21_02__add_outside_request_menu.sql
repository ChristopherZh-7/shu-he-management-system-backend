-- =====================================================
-- 外出请求菜单配置
-- 放置在「服务项发起」目录下，与「渗透测试发起」并行
-- =====================================================

-- 获取「服务项发起」目录的ID
SET @service_initiate_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '服务项发起' AND `parent_id` = 1185 AND `deleted` = b'0' LIMIT 1);

-- 在「服务项发起」目录下添加「外出请求发起」菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '外出请求发起', '', 2, 2, @service_initiate_menu_id, 'outside', 'ep:suitcase', 'project/outside/index', 'OutsideRequestList', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @service_initiate_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `name` = '外出请求发起' AND `parent_id` = @service_initiate_menu_id AND `deleted` = b'0'
);

-- 获取外出请求菜单ID
SET @outside_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '外出请求发起' AND `parent_id` = @service_initiate_menu_id AND `deleted` = b'0' LIMIT 1);

-- 添加外出请求的权限按钮（如果不存在）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '发起外出请求', 'project:outside-request:create', 3, 1, @outside_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @outside_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `permission` = 'project:outside-request:create' AND `parent_id` = @outside_menu_id AND `deleted` = b'0'
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查看外出请求', 'project:outside-request:query', 3, 2, @outside_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @outside_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `permission` = 'project:outside-request:query' AND `parent_id` = @outside_menu_id AND `deleted` = b'0'
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '修改外出请求', 'project:outside-request:update', 3, 3, @outside_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @outside_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `permission` = 'project:outside-request:update' AND `parent_id` = @outside_menu_id AND `deleted` = b'0'
);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '删除外出请求', 'project:outside-request:delete', 3, 4, @outside_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @outside_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `permission` = 'project:outside-request:delete' AND `parent_id` = @outside_menu_id AND `deleted` = b'0'
);

-- 为超级管理员角色分配外出请求菜单权限
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, @outside_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @outside_menu_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` WHERE `role_id` = 1 AND `menu_id` = @outside_menu_id AND `deleted` = b'0'
);
