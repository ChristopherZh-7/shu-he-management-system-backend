-- 添加首次登录强制修改密码字段
-- 0-不需要修改 1-必须修改（新用户、管理员重置密码后）
ALTER TABLE `system_users` ADD COLUMN `password_must_change` tinyint NOT NULL DEFAULT 0 COMMENT '是否首次登录需强制修改密码：0-否 1-是' AFTER `login_date`;
