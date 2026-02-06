-- =============================================
-- 钉钉通知场景配置菜单
-- =============================================

-- 查找钉钉群机器人菜单的父ID
SET @parent_menu_id = (
    SELECT parent_id FROM system_menu 
    WHERE name = '群机器人管理' AND deleted = 0 
    LIMIT 1
);

-- 如果没找到，使用系统管理菜单（ID=1）
SET @parent_menu_id = IFNULL(@parent_menu_id, 1);

-- 插入通知场景配置菜单
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`, `status`,
    `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
) VALUES (
    '通知场景配置', '', 2, 51, @parent_menu_id,
    'dingtalk-notification', 'ep:message-box', 'system/dingtalknotification/index', 'DingtalkNotification', 0,
    1, 1, 1, 'admin', NOW(),
    'admin', NOW(), 0
);

SET @notification_menu_id = LAST_INSERT_ID();

-- 插入子菜单（CRUD按钮）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
('通知配置查询', 'system:dingtalk-notification:query', 3, 1, @notification_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('通知配置创建', 'system:dingtalk-notification:create', 3, 2, @notification_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('通知配置更新', 'system:dingtalk-notification:update', 3, 3, @notification_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('通知配置删除', 'system:dingtalk-notification:delete', 3, 4, @notification_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);
