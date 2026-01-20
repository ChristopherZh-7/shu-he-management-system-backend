-- 为 system_users 表添加职级字段
-- 用于存储从钉钉同步的员工职级信息

ALTER TABLE `system_users` ADD COLUMN `position_level` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职级' AFTER `employee_status`;
