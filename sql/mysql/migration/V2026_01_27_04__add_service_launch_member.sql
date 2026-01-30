-- 服务发起执行人表
-- 用于记录每个执行人的确认状态、完成状态等（主要用于外出服务）

CREATE TABLE IF NOT EXISTS `project_service_launch_member` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `launch_id` bigint NOT NULL COMMENT '服务发起ID',
    `user_id` bigint NOT NULL COMMENT '执行人ID',
    `user_name` varchar(64) DEFAULT NULL COMMENT '执行人姓名（快照）',
    `user_dept_id` bigint DEFAULT NULL COMMENT '执行人部门ID（快照）',
    `user_dept_name` varchar(128) DEFAULT NULL COMMENT '执行人部门名称（快照）',
    `confirm_status` tinyint NOT NULL DEFAULT 0 COMMENT '确认状态：0未确认 1已确认 2已提交OA 3OA已通过',
    `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
    `oa_process_instance_id` varchar(128) DEFAULT NULL COMMENT '钉钉OA审批实例ID',
    `finish_status` tinyint NOT NULL DEFAULT 0 COMMENT '完成状态：0未完成 1已完成（无附件） 2已完成（有附件）',
    `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
    `attachment_url` text DEFAULT NULL COMMENT '附件URL（多个用逗号分隔）',
    `finish_remark` varchar(500) DEFAULT NULL COMMENT '完成备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_launch_id` (`launch_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_confirm_status` (`confirm_status`),
    KEY `idx_finish_status` (`finish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务发起执行人表';
