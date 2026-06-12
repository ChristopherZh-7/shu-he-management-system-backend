SELECT '== finance perms in menu ==';
SELECT id, name, permission, status, HEX(deleted) FROM system_menu WHERE permission LIKE 'finance:%';
SELECT '== roles having finance query ==';
SELECT rm.menu_id, rm.role_id, r.name
FROM system_role_menu rm
JOIN system_menu m ON m.id = rm.menu_id AND m.permission LIKE 'finance:%query%' AND m.deleted = 0
JOIN system_role r ON r.id = rm.role_id AND r.deleted = 0
WHERE rm.deleted = 0
ORDER BY rm.menu_id, rm.role_id;
SELECT '== finance menus visible ==';
SELECT id, name, permission, parent_id, status, HEX(deleted) FROM system_menu WHERE name LIKE '%预算%' OR name LIKE '%财务%';
