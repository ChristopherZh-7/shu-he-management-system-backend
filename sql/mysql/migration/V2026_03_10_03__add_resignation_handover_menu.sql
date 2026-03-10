-- =============================================
-- 离职交接菜单
-- 系统管理 -> 离职交接
-- =============================================

-- 离职交接主菜单（系统管理下，sort=2 放在用户管理后）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('离职交接', 'system:resignation-handover:query', 2, 2, 1, 'resignation-handover', 'ep:switch', 'system/resignation-handover/index', 'ResignationHandover', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

SET @handover_menu_id = LAST_INSERT_ID();

-- 预览权限（与主菜单共用 query）
-- 执行权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('执行交接', 'system:resignation-handover:execute', 3, 1, @handover_menu_id, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
