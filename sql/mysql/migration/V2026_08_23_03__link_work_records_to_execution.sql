-- 工作记录关联真实执行来源，并补充可量化的个人投入与产出字段。
-- 历史记录保持 source_type=manual、verification_status=0，不反推工时。

ALTER TABLE `project_management_record`
    MODIFY COLUMN `project_id` bigint DEFAULT NULL COMMENT '项目ID；内部工作可为空',
    MODIFY COLUMN `project_type` tinyint DEFAULT NULL COMMENT '项目类型；内部工作可为空',
    ADD COLUMN `source_type` varchar(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型: internal-内部工作 manual-项目手工 service_item-固定服务项 round-项目轮次 ticket-服务工单' AFTER `service_type`,
    ADD COLUMN `source_id` bigint DEFAULT NULL COMMENT '来源业务ID（轮次/工单/驻场记录）' AFTER `source_type`,
    ADD COLUMN `source_name` varchar(255) DEFAULT NULL COMMENT '来源名称快照' AFTER `source_id`,
    ADD COLUMN `actual_minutes` int DEFAULT NULL COMMENT '个人实际投入分钟数' AFTER `work_content`,
    ADD COLUMN `completion_percent` tinyint DEFAULT NULL COMMENT '本条工作完成比例0-100' AFTER `actual_minutes`,
    ADD COLUMN `work_result` text DEFAULT NULL COMMENT '工作结果/产出说明' AFTER `completion_percent`,
    ADD COLUMN `output_quantity` decimal(12,2) DEFAULT NULL COMMENT '产出数量' AFTER `work_result`,
    ADD COLUMN `output_unit` varchar(32) DEFAULT NULL COMMENT '产出单位，如个/台/份' AFTER `output_quantity`,
    ADD COLUMN `verification_status` tinyint NOT NULL DEFAULT 0 COMMENT '核验状态: 0-自报 1-已关联 2-已验收' AFTER `output_unit`,
    ADD KEY `idx_work_record_source` (`source_type`, `source_id`),
    ADD KEY `idx_work_record_verification` (`verification_status`);
