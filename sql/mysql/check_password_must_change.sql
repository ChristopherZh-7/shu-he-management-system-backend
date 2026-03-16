-- 诊断：检查用户 password_must_change 状态
-- 用于排查「首次登录需修改密码」弹窗不显示的问题

-- 1. 查看 system_users 表是否有 password_must_change 列
SHOW COLUMNS FROM system_users LIKE 'password_must_change';

-- 2. 查看所有用户的 password_must_change 值（1=需修改，0=不需要）
SELECT id, username, nickname, password_must_change, update_time
FROM system_users
ORDER BY id;

-- 3. 若需手动将某用户设为「需修改密码」，可执行：
-- UPDATE system_users SET password_must_change = 1 WHERE id = ?;
