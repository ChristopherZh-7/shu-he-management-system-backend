-- 诊断用户 226 访问 project/site/list-by-project 的权限
SET NAMES utf8mb4;

SELECT '===== 1. 用户 226 基本信息 =====' AS t;
SELECT id, username, nickname, dept_id FROM system_users WHERE id = 226 AND deleted = 0;

SELECT '===== 2. 用户 226 的角色 =====' AS t;
SELECT ur.role_id, r.name, r.code FROM system_user_role ur
JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id = 226 AND ur.deleted = 0;

SELECT '===== 3. project:site:query 菜单分布 =====' AS t;
SELECT m.id, m.permission, pm.path AS parent_path, pm.name AS parent_name
FROM system_menu m
JOIN system_menu pm ON pm.id = m.parent_id
WHERE m.permission = 'project:site:query' AND m.deleted = 0;

SELECT '===== 4. 用户226 是否拥有 project:site:query =====' AS t;
SELECT COUNT(*) AS has_site_query
FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 226 AND ur.deleted = 0 AND m.permission = 'project:site:query';

SELECT '===== 5. 用户226 拥有的 project 相关权限 =====' AS t;
SELECT DISTINCT m.permission
FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 226 AND ur.deleted = 0
  AND (m.permission LIKE 'project:%')
ORDER BY m.permission;
