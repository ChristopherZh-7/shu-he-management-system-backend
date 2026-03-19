-- 修复 ay_mg 访问 project/get 没权限
-- 直接为 ay_mg(163) 添加 project:project:query 权限（5166 安全运营下的菜单）

-- 1. 确保 5166 下有 project:project:query 菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:query' AND `parent_id` = 5166 AND `deleted` = 0);

-- 2. 为 ay_mg 添加该菜单（获取刚插入或已存在的 menu_id）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 163, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_menu m
WHERE m.permission = 'project:project:query' AND m.parent_id = 5166 AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = 163 AND rm.menu_id = m.id AND rm.deleted = 0);

SELECT 'fix_ay_mg_project_get_permission 完成。请执行 clear_permission_cache.sh 并让詹裕文重新登录。' AS result;
