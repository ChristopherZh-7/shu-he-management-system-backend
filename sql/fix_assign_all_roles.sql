-- 为所有拥有 安全服务/安全运营/数据安全 菜单的角色，强制分配 project:dept-service:query
-- 解决可能漏分配的情况

-- 安全服务(5081) -> 部门服务单查询(5239)
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT rm.role_id, 5239, '1', NOW(), '1', NOW(), 0
FROM system_role_menu rm
WHERE rm.menu_id = 5081 AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 5239 AND rm2.deleted = 0);

-- 安全运营(5166) -> 部门服务单查询(5245)
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT rm.role_id, 5245, '1', NOW(), '1', NOW(), 0
FROM system_role_menu rm
WHERE rm.menu_id = 5166 AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 5245 AND rm2.deleted = 0);

-- 数据安全(5091) -> 部门服务单查询(5246)
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT rm.role_id, 5246, '1', NOW(), '1', NOW(), 0
FROM system_role_menu rm
WHERE rm.menu_id = 5091 AND rm.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 5246 AND rm2.deleted = 0);

SELECT '已为所有相关角色补全权限' AS result;
