-- =====================================================
-- 外出请求功能
-- 用于跨部门人员借调/外出协助
-- =====================================================

-- 外出请求表
CREATE TABLE IF NOT EXISTS `project_outside_request` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '关联项目ID',
    `service_item_id` BIGINT DEFAULT NULL COMMENT '关联服务项ID（可选）',
    `request_user_id` BIGINT NOT NULL COMMENT '发起人ID',
    `request_dept_id` BIGINT DEFAULT NULL COMMENT '发起人部门ID',
    `target_dept_id` BIGINT NOT NULL COMMENT '目标部门ID（向哪个部门申请人员）',
    `destination` VARCHAR(500) DEFAULT NULL COMMENT '外出地点/客户现场',
    `reason` VARCHAR(1000) DEFAULT NULL COMMENT '外出事由',
    `plan_start_time` DATETIME DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` DATETIME DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` DATETIME DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` DATETIME DEFAULT NULL COMMENT '实际结束时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0待审批 1已通过 2已拒绝 3已完成 4已取消',
    `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_service_item_id` (`service_item_id`),
    KEY `idx_request_user_id` (`request_user_id`),
    KEY `idx_target_dept_id` (`target_dept_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外出请求表';

-- 外出人员表（审批通过时由目标部门负责人选择派谁去）
CREATE TABLE IF NOT EXISTS `project_outside_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_id` BIGINT NOT NULL COMMENT '外出请求ID',
    `user_id` BIGINT NOT NULL COMMENT '外出人员ID',
    `user_name` VARCHAR(100) DEFAULT NULL COMMENT '人员姓名（快照）',
    `user_dept_id` BIGINT DEFAULT NULL COMMENT '人员部门ID（快照）',
    `user_dept_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称（快照）',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_request_id` (`request_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外出人员表';
