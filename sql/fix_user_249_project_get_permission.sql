-- =============================================
-- 修复用户249（郑屹/af_mg）点击编辑时 project/project/get 返回 403
--
-- 原因：/project/project/get 需要 project:project:query 权限，
--       但安全服务(5081)下只有 project:info:query，af_mg 角色缺少 project:project:query
--
-- 本脚本：在安全服务下添加 project:project:query，并分配给 af_mg
-- =============================================

SET NAMES utf8mb4;

-- 1. 安全服务(5081)下添加 project:project:query（若不存在）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:query' AND `parent_id` = 5081 AND `deleted` = 0);

-- 2. 为 af_mg(164) 分配 project:project:query
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 164, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` m
WHERE m.permission = 'project:project:query' AND m.parent_id = 5081 AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = 164 AND rm.menu_id = m.id AND rm.deleted = 0);

-- 3. 若 5081 下仍无 project:project:query（可能 parent_id 不同），用 path 动态查找
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 
  (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0 LIMIT 1), '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL 
WHERE NOT EXISTS (SELECT 1 FROM system_menu m2 
  WHERE m2.permission = 'project:project:query' 
  AND m2.parent_id = (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0 LIMIT 1) 
  AND m2.deleted = 0);

-- 4. 为 af_mg 分配（动态 parent）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 164, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` m
WHERE m.permission = 'project:project:query' AND m.deleted = 0
  AND m.parent_id IN (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = 164 AND rm.menu_id = m.id AND rm.deleted = 0);

SELECT '修复完成！请清除 Redis 缓存并让用户 249 重新登录。' AS result;
