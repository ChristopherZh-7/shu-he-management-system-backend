-- 诊断 ay_mg 的 project 相关权限
SELECT 'ay_mg project 权限' AS title;
SELECT m.id, m.permission, m.parent_id, pm.name AS parent_name
FROM system_role_menu rm
JOIN system_menu m ON rm.menu_id = m.id AND m.deleted = 0
JOIN system_menu pm ON m.parent_id = pm.id AND pm.deleted = 0
JOIN system_role r ON rm.role_id = r.id AND r.deleted = 0
WHERE r.code = 'ay_mg' AND rm.deleted = 0
  AND m.permission LIKE 'project:%'
ORDER BY m.permission;

SELECT 'project:project:query 菜单(5166下)' AS title;
SELECT id, name, permission, parent_id FROM system_menu 
WHERE permission = 'project:project:query' AND parent_id = 5166 AND deleted = 0;

SELECT 'project:service-item:query 菜单' AS title;
SELECT id, name, permission, parent_id FROM system_menu 
WHERE permission = 'project:service-item:query' AND deleted = 0;
