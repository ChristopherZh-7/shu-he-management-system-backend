-- 员工工作排期表
-- 用于记录员工的工作安排，支持空置查看和排队机制

CREATE TABLE IF NOT EXISTS `employee_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '员工ID',
    `user_name` VARCHAR(64) DEFAULT NULL COMMENT '员工姓名（快照）',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `dept_name` VARCHAR(128) DEFAULT NULL COMMENT '部门名称（快照）',
    
    -- 关联信息
    `launch_id` BIGINT DEFAULT NULL COMMENT '关联服务申请ID',
    `round_id` BIGINT DEFAULT NULL COMMENT '关联轮次ID',
    `service_item_id` BIGINT DEFAULT NULL COMMENT '关联服务项ID',
    `project_id` BIGINT DEFAULT NULL COMMENT '关联项目ID',
    
    -- 排期信息
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0排队中 1进行中 2已完成 3已取消',
    `plan_start_time` DATETIME DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` DATETIME DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
    
    -- 排队信息
    `queue_order` INT DEFAULT NULL COMMENT '排队顺序（排队中时有效）',
    `expected_start_time` DATETIME DEFAULT NULL COMMENT '期望开始时间（申请人填写）',
    
    -- 任务描述
    `task_type` VARCHAR(32) DEFAULT NULL COMMENT '任务类型：cross_dept=跨部门服务 local=本部门服务',
    `task_description` VARCHAR(512) DEFAULT NULL COMMENT '任务描述',
    
    -- 审计字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`),
    KEY `idx_launch_id` (`launch_id`),
    KEY `idx_round_id` (`round_id`),
    KEY `idx_plan_time` (`plan_start_time`, `plan_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工工作排期表';

-- 服务发起表增加字段
ALTER TABLE `project_service_launch` 
    ADD COLUMN `is_queued` TINYINT NOT NULL DEFAULT 0 COMMENT '是否排队：0否 1是' AFTER `is_cross_dept`,
    ADD COLUMN `expected_executor_id` BIGINT DEFAULT NULL COMMENT '期望执行人ID' AFTER `is_queued`,
    ADD COLUMN `expected_executor_name` VARCHAR(64) DEFAULT NULL COMMENT '期望执行人姓名' AFTER `expected_executor_id`,
    ADD COLUMN `expected_start_time` DATETIME DEFAULT NULL COMMENT '期望开始时间' AFTER `expected_executor_name`,
    ADD COLUMN `queue_order` INT DEFAULT NULL COMMENT '排队顺序' AFTER `expected_start_time`;

-- 添加索引
ALTER TABLE `project_service_launch` ADD INDEX `idx_is_queued` (`is_queued`);
ALTER TABLE `project_service_launch` ADD INDEX `idx_expected_executor_id` (`expected_executor_id`);
