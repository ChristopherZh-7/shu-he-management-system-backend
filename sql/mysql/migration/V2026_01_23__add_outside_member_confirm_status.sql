-- 钉钉互动卡片与OA审批集成 - 外出人员确认状态字段
-- 为外出人员表添加确认状态、OA审批实例ID、确认时间字段

ALTER TABLE `project_outside_member` 
ADD COLUMN `confirm_status` TINYINT DEFAULT 0 COMMENT '确认状态：0未确认 1已确认 2已提交OA 3OA已通过',
ADD COLUMN `oa_process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '钉钉OA审批实例ID',
ADD COLUMN `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间';

-- 添加索引以便快速查询未确认的外出人员
CREATE INDEX `idx_confirm_status` ON `project_outside_member` (`confirm_status`);
