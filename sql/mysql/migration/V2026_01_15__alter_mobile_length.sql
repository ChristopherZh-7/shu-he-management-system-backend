-- 修复：扩展 mobile 字段长度以支持带国际区号的手机号格式（如 +86-17683991468）
-- 原长度：11，新长度：20

ALTER TABLE `system_users` MODIFY COLUMN `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '手机号码';

ALTER TABLE `system_sms_code` MODIFY COLUMN `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号';

ALTER TABLE `system_sms_log` MODIFY COLUMN `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号';
