-- 检查哪些角色拥有 project:dept-service:query
SELECT '=== 拥有 project:dept-service:query 的角色 ===' AS info;
SELECT r.id, r.name, r.code, rm.menu_id, m.name AS menu_name, m.permission
FROM system_role r
JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE r.deleted = 0 AND m.permission = 'project:dept-service:query'
ORDER BY r.id;

-- 检查拥有 安全服务 菜单的角色
SELECT '=== 拥有 安全服务(5081) 菜单的角色 ===' AS info;
SELECT r.id, r.name, r.code
FROM system_role r
JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = 0
WHERE rm.menu_id = 5081 AND rm.deleted = 0 AND r.deleted = 0;
