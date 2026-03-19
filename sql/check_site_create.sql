SELECT 'project:site:create/update/delete under three modules' AS t;
SELECT m.id, m.permission, pm.path AS parent_path
FROM system_menu m
JOIN system_menu pm ON pm.id = m.parent_id
WHERE m.permission IN ('project:site:create','project:site:update','project:site:delete')
  AND m.deleted = 0 AND pm.path IN ('security-service','security-operation','data-security')
ORDER BY pm.path, m.permission;
