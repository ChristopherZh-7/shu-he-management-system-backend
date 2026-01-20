-- 服务项增加频次配置字段
-- 频次类型 + 最大次数 组合控制（合同期内）：
--   0-按需：不限制
--   1-按月：合同期内最多 max_count 次
--   2-按季：合同期内最多 max_count 次  
--   3-按年：合同期内最多 max_count 次
-- 频次类型只是标签说明服务周期，真正的限制是 max_count

-- 1. 服务项表增加频次配置字段
ALTER TABLE `project_info` 
ADD COLUMN `frequency_type` tinyint DEFAULT 0 COMMENT '频次类型：0按需(不限) 1按月 2按季 3按年' AFTER `amount`,
ADD COLUMN `max_count` int DEFAULT 1 COMMENT '每周期最大执行次数（按需时无效）' AFTER `frequency_type`,
ADD COLUMN `used_count` int DEFAULT 0 COMMENT '历史执行总次数（用于统计）' AFTER `max_count`;

-- 2. 项目轮次表增加服务项关联和工作流关联
ALTER TABLE `project_round` 
ADD COLUMN `service_item_id` bigint DEFAULT NULL COMMENT '服务项ID' AFTER `project_id`,
ADD COLUMN `process_instance_id` varchar(64) DEFAULT NULL COMMENT '工作流流程实例ID' AFTER `service_item_id`;

-- 3. 添加索引
ALTER TABLE `project_round` ADD INDEX `idx_service_item_id` (`service_item_id`);
ALTER TABLE `project_round` ADD INDEX `idx_process_instance_id` (`process_instance_id`);

-- 4. 更新现有数据：将已有轮次关联到对应的服务项
-- 根据 project_id 查找对应的服务项，然后关联
UPDATE `project_round` pr
INNER JOIN `project_info` pi ON pr.project_id = pi.project_id
SET pr.service_item_id = pi.id
WHERE pr.service_item_id IS NULL;
