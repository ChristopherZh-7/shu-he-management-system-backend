-- =====================================================
-- 工单中心模块 - 补「工单详情」隐藏菜单（侧栏不显示，仅用于路由匹配）
-- 关联设计：docs/design/ticket-design.md §3
-- =====================================================

-- 取顶级目录「工单中心」ID
SET @ticket_top = (SELECT `id` FROM `system_menu` WHERE `name`='工单中心' AND `parent_id`=0 AND `deleted`=b'0' LIMIT 1);

-- 1. 工单详情菜单（visible=b'0'，侧栏不显示；前端按 path 匹配路由）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '工单详情', 'ticket:ticket:query', 2, 99, @ticket_top, 'detail/:id', '', 'ticket/detail/index', 'TicketDetail', 0, b'0', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @ticket_top IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM `system_menu`
        WHERE `name`='工单详情' AND `parent_id`=@ticket_top AND `deleted`=b'0'
  );

-- 2. 给超级管理员（role_id=1）授权该菜单
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0', 1
FROM `system_menu` m
WHERE m.`name` = '工单详情'
  AND m.`parent_id` = @ticket_top
  AND m.`deleted` = b'0'
  AND NOT EXISTS (
        SELECT 1 FROM `system_role_menu` rm
        WHERE rm.role_id = 1 AND rm.menu_id = m.id AND rm.`deleted` = b'0'
  );
