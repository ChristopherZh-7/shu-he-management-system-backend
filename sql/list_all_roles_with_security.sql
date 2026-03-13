-- 列出所有角色及其是否拥有 安全服务/ project:dept-service:query
SELECT r.id, r.name, r.code,
  MAX(CASE WHEN rm.menu_id = 5081 THEN 1 ELSE 0 END) AS has_security_service,
  MAX(CASE WHEN m.permission = 'project:dept-service:query' THEN 1 ELSE 0 END) AS has_dept_query
FROM system_role r
LEFT JOIN system_role_menu rm ON rm.role_id = r.id AND rm.deleted = 0
LEFT JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0 AND m.permission = 'project:dept-service:query'
WHERE r.deleted = 0
GROUP BY r.id, r.name, r.code
ORDER BY r.id;
