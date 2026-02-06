-- =============================================
-- 钉钉群机器人菜单配置
-- =============================================

-- 获取系统管理菜单ID（假设是1号菜单）
SET @system_menu_id = 1;

-- 查找钉钉配置菜单的父ID（如果存在钉钉相关菜单则放在同级）
SET @parent_menu_id = (
    SELECT IFNULL(
        (SELECT parent_id FROM system_menu WHERE name = '钉钉配置' AND deleted = 0 LIMIT 1),
        @system_menu_id
    )
);

-- 插入群机器人管理菜单
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`, `status`,
    `visible`, `keep_alive`, `always_show`, `creator`, `create_time`,
    `updater`, `update_time`, `deleted`
) VALUES (
    '群机器人管理', '', 2, 50, @parent_menu_id,
    'dingtalk-robot', 'fa:robot', 'system/dingtalkrobot/index', 'DingtalkRobot', 0,
    1, 1, 1, 'admin', NOW(),
    'admin', NOW(), 0
);

SET @robot_menu_id = LAST_INSERT_ID();

-- 插入群机器人子菜单（CRUD按钮）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
('群机器人查询', 'system:dingtalk-robot:query', 3, 1, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('群机器人创建', 'system:dingtalk-robot:create', 3, 2, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('群机器人更新', 'system:dingtalk-robot:update', 3, 3, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('群机器人删除', 'system:dingtalk-robot:delete', 3, 4, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('群机器人导出', 'system:dingtalk-robot:export', 3, 5, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0),
('群机器人发送', 'system:dingtalk-robot:send', 3, 6, @robot_menu_id, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);
