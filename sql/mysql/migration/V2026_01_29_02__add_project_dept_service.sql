-- 新增部门服务单表
-- 用于解决一个项目被多个部门服务时，负责人、状态、进度等字段冲突的问题
-- 每个部门有独立的服务单记录，可以独立管理状态和负责人

CREATE TABLE `project_dept_service` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 关联信息
    `project_id` bigint NOT NULL COMMENT '所属项目ID',
    `contract_id` bigint DEFAULT NULL COMMENT 'CRM合同ID',
    `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号',
    `customer_id` bigint DEFAULT NULL COMMENT 'CRM客户ID',
    `customer_name` varchar(255) DEFAULT NULL COMMENT '客户名称',
    
    -- 部门信息
    `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID（领取后填充）',
    `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
    `dept_type` tinyint NOT NULL COMMENT '部门类型：1-安全服务 2-安全运营 3-数据安全',
    
    -- 领取信息
    `claim_user_id` bigint DEFAULT NULL COMMENT '领取人ID',
    `claim_user_name` varchar(100) DEFAULT NULL COMMENT '领取人姓名',
    `claim_time` datetime DEFAULT NULL COMMENT '领取时间',
    `claimed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已领取：0-否 1-是',
    
    -- 负责人（独立于其他部门）
    `manager_ids` json DEFAULT NULL COMMENT '负责人ID列表（JSON数组）',
    `manager_names` json DEFAULT NULL COMMENT '负责人姓名列表（JSON数组）',
    
    -- 状态和进度（每个部门独立）
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待领取 1-进行中 2-已暂停 3-已完成 4-已取消',
    `progress` int DEFAULT 0 COMMENT '进度百分比 0-100',
    
    -- 时间信息
    `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    
    -- 扩展
    `description` varchar(1000) DEFAULT NULL COMMENT '描述',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_dept_type` (`dept_type`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目-部门服务单';

-- 为服务项表增加部门服务单关联字段
ALTER TABLE `project_info` 
ADD COLUMN `dept_service_id` bigint DEFAULT NULL COMMENT '所属部门服务单ID' AFTER `project_id`;

ALTER TABLE `project_info` 
ADD INDEX `idx_dept_service_id` (`dept_service_id`);
