-- =====================================================
-- 工单中心模块 - 菜单 + 按钮权限 + 超级管理员授权
-- =====================================================

-- 1. 顶级目录「工单中心」
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单中心', '', 1, 90, 0, '/ticket', 'ep:tickets', 'Layout', 'TicketCenter', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name`='工单中心' AND `parent_id`=0 AND `deleted`=b'0');

SET @ticket_top = (SELECT `id` FROM `system_menu` WHERE `name`='工单中心' AND `parent_id`=0 AND `deleted`=b'0' LIMIT 1);

-- 2. 子菜单：工单列表
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单列表', '', 2, 1, @ticket_top, 'list', 'ep:list', 'ticket/list/index', 'TicketList', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @ticket_top IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name`='工单列表' AND `parent_id`=@ticket_top AND `deleted`=b'0');

SET @ticket_list = (SELECT `id` FROM `system_menu` WHERE `name`='工单列表' AND `parent_id`=@ticket_top AND `deleted`=b'0' LIMIT 1);

-- 3. 工单按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '创建工单', 'ticket:ticket:create', 3, 1, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:create' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '查询工单', 'ticket:ticket:query', 3, 2, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:query' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '修改工单', 'ticket:ticket:update', 3, 3, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:update' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '删除工单', 'ticket:ticket:delete', 3, 4, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:delete' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '分派工单', 'ticket:ticket:assign', 3, 5, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:assign' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '完成工单', 'ticket:ticket:finish', 3, 6, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:finish' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '关闭工单', 'ticket:ticket:close', 3, 7, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:close' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '转交工单', 'ticket:ticket:transfer', 3, 8, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:ticket:transfer' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单评论', 'ticket:comment:create', 3, 9, @ticket_list, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_list IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:comment:create' AND `deleted`=b'0');

-- 4. 子菜单：我的工单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的工单', '', 2, 2, @ticket_top, 'my', 'ep:user', 'ticket/my/index', 'MyTicket', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @ticket_top IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name`='我的工单' AND `parent_id`=@ticket_top AND `deleted`=b'0');

-- 5. 子菜单：工单分类（管理员）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单分类', 'ticket:category:query', 2, 3, @ticket_top, 'category', 'ep:folder', 'ticket/category/index', 'TicketCategory', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @ticket_top IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `name`='工单分类' AND `parent_id`=@ticket_top AND `deleted`=b'0');

SET @ticket_cat = (SELECT `id` FROM `system_menu` WHERE `name`='工单分类' AND `parent_id`=@ticket_top AND `deleted`=b'0' LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '分类管理', 'ticket:category:create', 3, 1, @ticket_cat, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_cat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:category:create' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '分类修改', 'ticket:category:update', 3, 2, @ticket_cat, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_cat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:category:update' AND `deleted`=b'0');

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '分类删除', 'ticket:category:delete', 3, 3, @ticket_cat, 0, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @ticket_cat IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission`='ticket:category:delete' AND `deleted`=b'0');

-- 6. 给超级管理员（role_id=1）授权整个工单中心目录及全部子菜单
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE (
        m.id = @ticket_top
        OR m.parent_id = @ticket_top
        OR m.parent_id IN (
            SELECT id FROM (
                SELECT id FROM `system_menu` WHERE parent_id = @ticket_top AND `deleted` = b'0'
            ) t
        )
      )
  AND m.`deleted` = b'0'
  AND NOT EXISTS (
        SELECT 1 FROM `system_role_menu` rm
        WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.`deleted` = b'0'
  );
