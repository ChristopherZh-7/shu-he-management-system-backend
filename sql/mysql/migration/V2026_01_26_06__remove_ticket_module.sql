-- =====================================================
-- 删除工单管理模块
-- =====================================================

-- 删除工单管理相关菜单（先删除子菜单再删除父菜单）
-- 获取工单管理菜单ID
SET @ticket_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '工单管理' AND `deleted` = b'0' LIMIT 1);

-- 删除工单管理下的所有子菜单（包括按钮权限）
UPDATE `system_menu` SET `deleted` = b'1', `update_time` = NOW() 
WHERE `parent_id` IN (SELECT id FROM (SELECT `id` FROM `system_menu` WHERE `parent_id` = @ticket_menu_id AND `deleted` = b'0') AS temp);

-- 删除工单管理的直接子菜单
UPDATE `system_menu` SET `deleted` = b'1', `update_time` = NOW() 
WHERE `parent_id` = @ticket_menu_id AND `deleted` = b'0';

-- 删除工单管理菜单本身
UPDATE `system_menu` SET `deleted` = b'1', `update_time` = NOW() 
WHERE `id` = @ticket_menu_id AND `deleted` = b'0';

-- 删除角色菜单关联
DELETE FROM `system_role_menu` WHERE `menu_id` IN (
    SELECT id FROM (
        SELECT `id` FROM `system_menu` WHERE `name` LIKE '%工单%' OR `path` LIKE '%ticket%'
    ) AS temp
);

-- 可选：删除工单相关数据表（如果需要完全清理）
-- DROP TABLE IF EXISTS `ticket_log`;
-- DROP TABLE IF EXISTS `ticket`;
-- DROP TABLE IF EXISTS `ticket_category`;
