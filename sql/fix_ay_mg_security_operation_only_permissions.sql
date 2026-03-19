-- =============================================
-- 修复安全运营主管(ay_mg) 仅能管理安全运营的权限
--
-- 背景：先前给 ay_mg 加了安全服务权限，调整回去后 ay_mg 无法查看安全运营项目详情。
--       原因：安全运营详情页复用了 project/project/get、service-item/manager-list-by-dept-type 等接口，
--       需要 project:project:query、project:service-item:query。这些权限在安全运营(5166)下也有对应菜单。
--
-- 本脚本：为 ay_mg 补充 安全运营(5166) 下的 project:project:query、project:service-item:query 等
--        接口权限，使其能正常打开安全运营项目详情，但不给安全服务(5081)菜单。
-- =============================================

SET NAMES utf8mb4;

-- 1. 安全运营(5166)下若无 project:service-item:query，则添加
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '服务项查询', 'project:service-item:query', 3, 22, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:service-item:query' AND `parent_id` = 5166 AND `deleted` = 0);

-- 2. 为 ay_mg 补充 5166 下的 project:project:query、project:service-item:query、project:dept-service:query 等
--    （仅补充 5166 下的，不涉及 5081 安全服务）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_role r
CROSS JOIN system_menu m
WHERE r.code = 'ay_mg' AND r.deleted = 0
  AND m.parent_id = 5166 AND m.type = 3 AND m.deleted = 0
  AND m.permission IN (
    'project:project:query',
    'project:service-item:query',
    'project:dept-service:query',
    'project:dept-service:create',
    'project:dept-service:update',
    'project:dept-service:delete',
    'project:site:query',
    'project:site:create',
    'project:site:update',
    'project:site:delete'
  )
  AND NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = 0
  );

SELECT 'fix_ay_mg_security_operation_only_permissions 完成。请让安全运营主管重新登录后测试。' AS result;
