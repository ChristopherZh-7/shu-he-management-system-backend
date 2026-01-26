-- =====================================================
-- 创建服务执行申请表
-- =====================================================

CREATE TABLE IF NOT EXISTS `project_service_execution` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` bigint NOT NULL COMMENT '关联项目ID',
    `service_item_id` bigint NOT NULL COMMENT '关联服务项ID',
    `request_user_id` bigint NOT NULL COMMENT '发起人ID',
    `request_dept_id` bigint DEFAULT NULL COMMENT '发起人部门ID',
    `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `executor_ids` varchar(500) DEFAULT NULL COMMENT '执行人ID列表（JSON数组）',
    `executor_names` varchar(500) DEFAULT NULL COMMENT '执行人姓名列表（逗号分隔）',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0待审批 1已通过 2已拒绝 3已取消',
    `process_instance_id` varchar(64) DEFAULT NULL COMMENT '工作流流程实例ID',
    `round_id` bigint DEFAULT NULL COMMENT '创建的轮次ID（审批通过后）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_service_item_id` (`service_item_id`),
    KEY `idx_request_user_id` (`request_user_id`),
    KEY `idx_process_instance_id` (`process_instance_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务执行申请表';
