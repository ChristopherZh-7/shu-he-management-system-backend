-- =============================================
-- 检查指定账号是否具备 CRM 待办事项所需权限
--
-- 使用方法：修改下面 @check_username 为你的登录用户名，然后执行本脚本
-- =============================================

SET NAMES utf8mb4;

-- 改成你的登录用户名（如 admin、你的姓名拼音等）
SET @check_username = 'zhengyi';

-- 查询用户信息
SELECT '=== 用户信息 ===' AS '';
SELECT id AS user_id, username, nickname, dept_id
FROM system_users
WHERE username = @check_username AND deleted = 0;

-- 用户角色
SELECT '=== 用户角色 ===' AS '';
SELECT r.id AS role_id, r.name AS role_name
FROM system_role r
JOIN system_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
WHERE u.username = @check_username;

-- 检查是否拥有待办菜单及 5 个权限
SELECT '=== 待办事项权限检查 ===' AS '';
SELECT
  req.permission,
  req.menu_name,
  IF(EXISTS (
    SELECT 1 FROM system_role_menu rm
    JOIN system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
    JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
    WHERE rm.menu_id = req.menu_id AND rm.deleted = 0 AND u.username = @check_username
  ), 'YES', 'NO') AS has_permission
FROM (
  SELECT 2701 AS menu_id, '(菜单)' AS permission, '待办事项' AS menu_name
  UNION ALL (SELECT id, permission, name FROM system_menu WHERE permission = 'crm:customer:query' AND deleted = 0 LIMIT 1)
  UNION ALL (SELECT id, permission, name FROM system_menu WHERE permission = 'crm:clue:query' AND deleted = 0 LIMIT 1)
  UNION ALL (SELECT id, permission, name FROM system_menu WHERE permission = 'crm:contract:query' AND deleted = 0 LIMIT 1)
  UNION ALL (SELECT id, permission, name FROM system_menu WHERE permission = 'crm:receivable:query' AND deleted = 0 LIMIT 1)
  UNION ALL (SELECT id, permission, name FROM system_menu WHERE permission = 'crm:receivable-plan:query' AND deleted = 0 LIMIT 1)
) req
ORDER BY req.menu_id;

-- 汇总：是否缺权限
SELECT '=== 结论 ===' AS '';
SELECT
  CASE
    WHEN missing.cnt > 0 THEN CONCAT('缺少 ', missing.cnt, ' 个权限，需要执行 fix_crm_backlog_permissions.sql')
    ELSE '权限完整，无需修复'
  END AS result
FROM (
  SELECT COUNT(*) AS cnt
  FROM (
    SELECT 2701 AS id
    UNION (SELECT id FROM system_menu WHERE permission = 'crm:customer:query' AND deleted = 0 LIMIT 1)
    UNION (SELECT id FROM system_menu WHERE permission = 'crm:clue:query' AND deleted = 0 LIMIT 1)
    UNION (SELECT id FROM system_menu WHERE permission = 'crm:contract:query' AND deleted = 0 LIMIT 1)
    UNION (SELECT id FROM system_menu WHERE permission = 'crm:receivable:query' AND deleted = 0 LIMIT 1)
    UNION (SELECT id FROM system_menu WHERE permission = 'crm:receivable-plan:query' AND deleted = 0 LIMIT 1)
  ) req
  WHERE NOT EXISTS (
    SELECT 1 FROM system_role_menu rm
    JOIN system_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
    JOIN system_users u ON u.id = ur.user_id AND u.deleted = 0
    WHERE rm.menu_id = req.id AND rm.deleted = 0 AND u.username = @check_username
  )
) missing;
