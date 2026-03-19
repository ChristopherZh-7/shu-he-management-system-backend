-- 验证三个模块的 project:project:query 及角色分配
SELECT 'project:project:query 菜单分布' AS t;
SELECT m.id, m.name, m.permission, pm.path AS parent_path
FROM system_menu m
JOIN system_menu pm ON pm.id = m.parent_id
WHERE m.permission = 'project:project:query' AND m.deleted = 0
  AND pm.path IN ('security-service','security-operation','data-security');

SELECT '拥有 project:project:query 的角色' AS t;
SELECT r.id, r.name, r.code, pm.path AS module
FROM system_role_menu rm
JOIN system_menu m ON m.id = rm.menu_id
JOIN system_menu pm ON pm.id = m.parent_id
JOIN system_role r ON r.id = rm.role_id
WHERE m.permission = 'project:project:query' AND rm.deleted = 0
  AND pm.path IN ('security-service','security-operation','data-security')
ORDER BY pm.path, r.code;
