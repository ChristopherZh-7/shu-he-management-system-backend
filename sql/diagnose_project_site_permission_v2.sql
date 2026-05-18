-- ============================================================
-- 诊断 /project/manage/N 「没有权限」问题
-- 跑这个脚本看 3 个结果，能定位是 migration / 用户 / 角色哪一环出问题
-- ============================================================

-- (1) 看 V2026_05_18_02 是否跑了：5080 下应有 8 条新按钮（site×4 + dept-service×4）
SELECT '=== (1) 5080 下的 project:site:* + project:dept-service:* 应有 8 条 ===' AS step;
SELECT id, name, permission, parent_id, sort, CAST(deleted AS UNSIGNED) AS deleted
  FROM system_menu
 WHERE parent_id = 5080
   AND permission IN (
       'project:site:query','project:site:create','project:site:update','project:site:delete',
       'project:dept-service:query','project:dept-service:create','project:dept-service:update','project:dept-service:delete'
   )
 ORDER BY deleted, sort;

-- (2) 你当前登录账号的用户 ID = ?
--     请把下面的 999 改成你自己的 userId（不知道的话执行：
--     SELECT id, username, nickname FROM system_users WHERE username='你的账号' AND deleted = 0;）
SET @my_user_id = 1;  -- 默认 admin=1，请改成你登录的账号 ID

SELECT CONCAT('=== (2) 用户 ', @my_user_id, ' 的角色列表 ===') AS step;
SELECT u.id AS user_id, u.username, r.id AS role_id, r.code, r.name, r.code = 'super_admin' AS is_super_admin
  FROM system_users u
  JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
  JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0
 WHERE u.id = @my_user_id;

-- (3) 你的角色拥有的 project:site:query 权限数（应 ≥ 1）
SELECT CONCAT('=== (3) 用户 ', @my_user_id, ' 拥有 project:site:query 权限的菜单数 ===') AS step;
SELECT COUNT(*) AS owned_site_query_count
  FROM system_user_role ur
  JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
  JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
 WHERE ur.user_id = @my_user_id
   AND ur.deleted = 0
   AND m.permission = 'project:site:query';

-- (4) 如果 (3) 返回 0 且 (2) 也不是 super_admin → 手动授权
--     取消下面注释，把 @my_role_id 换成 (2) 查到的 role_id
-- SET @my_role_id = 100;
-- INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
-- SELECT @my_role_id, m.id, '1', NOW(), '1', NOW(), b'0'
--   FROM system_menu m
--  WHERE m.parent_id = 5080
--    AND m.type = 3
--    AND m.permission IN (
--        'project:project:query','project:project:create','project:project:update','project:project:delete',
--        'project:site:query','project:site:create','project:site:update','project:site:delete',
--        'project:dept-service:query','project:dept-service:create','project:dept-service:update','project:dept-service:delete'
--    )
--    AND m.deleted = b'0';
