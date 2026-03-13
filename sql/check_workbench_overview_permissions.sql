-- =============================================
-- 检查指定用户是否具备工作台「团队工作总览」「全局总览」所需权限
--
-- 使用前请将下方 @username 改为要检查的用户名（如总经办账号）
-- =============================================

SET @username = 'admin';  -- 改为要检查的用户名

-- 所需权限
SET @team_overview_query = 'project:team-overview:query';
SET @global_overview_query = 'project:global-overview:query';
SET @daily_record_query = 'project:daily-record:query';

SELECT '===== 用户信息 =====' AS title;
SELECT u.id, u.username, u.nickname, u.dept_id, d.name AS dept_name
FROM system_users u
LEFT JOIN system_dept d ON d.id = u.dept_id AND d.deleted = 0
WHERE u.username = @username AND u.deleted = 0;

SELECT '===== 用户角色 =====' AS title;
SELECT r.id, r.name AS role_name, r.code AS role_code
FROM system_role r
JOIN system_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
WHERE u.username = @username;

SELECT '===== 权限检查（团队工作总览/全局总览所需） =====' AS title;
SELECT
    m.permission,
    CASE WHEN rm.role_id IS NOT NULL THEN '✅ 有' ELSE '❌ 无' END AS has_permission
FROM (
    SELECT 1 AS ord, @team_overview_query AS permission
    UNION SELECT 2, @global_overview_query
    UNION SELECT 3, @daily_record_query
) AS req
JOIN system_menu m ON m.permission = req.permission AND m.deleted = 0
LEFT JOIN (
    SELECT rm.menu_id, rm.role_id
    FROM system_role_menu rm
    JOIN system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
    JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
    WHERE u.username = @username AND rm.deleted = 0
) rm ON rm.menu_id = m.id
ORDER BY req.ord;

SELECT '===== 工作台相关菜单 =====' AS title;
SELECT m.id, m.name, m.path, m.permission,
       CASE WHEN rm.role_id IS NOT NULL THEN '✅ 有' ELSE '❌ 无' END AS has_menu
FROM system_menu m
LEFT JOIN (
    SELECT rm.menu_id, rm.role_id
    FROM system_role_menu rm
    JOIN system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
    JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
    WHERE u.username = @username AND rm.deleted = 0
) rm ON rm.menu_id = m.id
WHERE m.deleted = 0
  AND (m.path IN ('workbench', 'team-overview', 'global-overview') OR m.name = '工作台')
ORDER BY m.parent_id, m.sort;
