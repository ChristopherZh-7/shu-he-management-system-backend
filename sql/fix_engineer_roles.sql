-- 为所有角色补充 project:dept-service:query（安全服务列表 API 必需）
-- 确保任何能进安全服务页面的用户都能调接口
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT r.id, 5239, '1', NOW(), '1', NOW(), 0
FROM system_role r
WHERE r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = 5239 AND rm.deleted = 0);

SELECT '已为所有角色补充 project:dept-service:query' AS result;
