-- 给轮次表添加外出和跨部门标识

ALTER TABLE `project_round`
    ADD COLUMN `is_outside` tinyint(1) DEFAULT 0 COMMENT '是否外出：0否 1是' AFTER `remark`,
    ADD COLUMN `is_cross_dept` tinyint(1) DEFAULT 0 COMMENT '是否跨部门：0否 1是' AFTER `is_outside`,
    ADD COLUMN `service_launch_id` bigint DEFAULT NULL COMMENT '关联的服务发起ID' AFTER `is_cross_dept`;

-- 添加索引
ALTER TABLE `project_round`
    ADD KEY `idx_is_outside` (`is_outside`),
    ADD KEY `idx_service_launch_id` (`service_launch_id`);
