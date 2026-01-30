-- 为外出费用记录表添加 service_launch_id 字段
-- 用于关联统一服务发起（替代旧的 outside_request_id）

ALTER TABLE `outside_cost_record` 
ADD COLUMN `service_launch_id` bigint DEFAULT NULL COMMENT '服务发起ID（统一服务发起）' AFTER `outside_request_id`;

-- 添加索引
ALTER TABLE `outside_cost_record` 
ADD INDEX `idx_service_launch_id` (`service_launch_id`);

-- 修改 outside_request_id 为可空（因为新的费用记录会使用 service_launch_id）
ALTER TABLE `outside_cost_record` 
MODIFY COLUMN `outside_request_id` bigint DEFAULT NULL COMMENT '外出申请ID（旧版，已废弃）';
