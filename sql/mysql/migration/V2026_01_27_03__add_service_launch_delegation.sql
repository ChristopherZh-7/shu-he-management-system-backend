-- 添加代发起相关字段
-- 代发起场景：当前用户帮其他部门发起该部门的服务项

ALTER TABLE `project_service_launch`
    ADD COLUMN `is_delegation` tinyint(1) DEFAULT 0 COMMENT '是否代发起：0否 1是' AFTER `is_cross_dept`,
    ADD COLUMN `delegate_user_id` bigint DEFAULT NULL COMMENT '被代发起人ID（代发起时必填）' AFTER `is_delegation`;

-- 添加索引
ALTER TABLE `project_service_launch`
    ADD KEY `idx_is_delegation` (`is_delegation`),
    ADD KEY `idx_delegate_user_id` (`delegate_user_id`);
