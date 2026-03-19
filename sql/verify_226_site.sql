SELECT 'user 226 has project:site:query' AS t;
SELECT COUNT(*) FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 226 AND ur.deleted = 0 AND m.permission = 'project:site:query';
