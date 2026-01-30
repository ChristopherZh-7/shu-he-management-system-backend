-- 删除 security_operation_contract 表的冗余负责人字段
-- 安全运营项目的负责人现在存储在 security_operation_member 表（is_leader=1）
-- 注意：project 表的 manager_ids/manager_names 字段如果不存在则无需处理

ALTER TABLE `security_operation_contract` DROP COLUMN `manager_id`;
ALTER TABLE `security_operation_contract` DROP COLUMN `manager_name`;
