-- 检查安全服务相关权限
SELECT '=== 安全服务/安全运营/数据安全 菜单及按钮 ===' AS info;
SELECT m.id, m.name, m.permission, m.type, pm.name AS parent_name
FROM system_menu m
LEFT JOIN system_menu pm ON pm.id = m.parent_id
WHERE m.deleted = 0 
  AND (m.permission LIKE 'project:dept-service%' OR m.permission LIKE 'project:info%' 
       OR m.path IN ('security-service','security-operation','data-security'))
ORDER BY m.parent_id, m.sort;

SELECT '=== project:dept-service:query 是否存在 ===' AS info;
SELECT id, name, permission, parent_id FROM system_menu 
WHERE permission = 'project:dept-service:query' AND deleted = 0;
