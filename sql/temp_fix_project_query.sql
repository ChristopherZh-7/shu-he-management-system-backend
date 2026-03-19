INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'project:project:query' AND parent_id = 5166 AND deleted = 0);

INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 163, m.id, '1', NOW(), '1', NOW(), b'0'
FROM system_menu m
WHERE m.permission = 'project:project:query' AND m.parent_id = 5166 AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm WHERE rm.role_id = 163 AND rm.menu_id = m.id AND rm.deleted = 0);
