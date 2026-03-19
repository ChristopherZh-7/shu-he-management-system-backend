-- 检查安全服务(5081)下是否有 project:project:query，以及 af_mg(164) 是否拥有
SELECT 'project:project:query under 5081' AS t;
SELECT id, name, permission, parent_id FROM system_menu 
WHERE permission = 'project:project:query' AND parent_id = 5081 AND deleted = 0;

SELECT 'af_mg(164) has project:project:query' AS t;
SELECT rm.role_id, rm.menu_id, m.permission 
FROM system_role_menu rm 
JOIN system_menu m ON m.id = rm.menu_id 
WHERE rm.role_id = 164 AND m.permission = 'project:project:query' AND rm.deleted = 0;
