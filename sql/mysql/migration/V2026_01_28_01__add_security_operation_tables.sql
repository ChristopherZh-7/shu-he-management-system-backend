-- 安全运营模块表结构
-- 安全运营 = 管理费 + 驻场费
-- 服务项来自合同，由驻场人员执行

-- 1. 安全运营合同表
-- 记录安全运营部承接的合同信息
CREATE TABLE IF NOT EXISTS `security_operation_contract` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 合同关联
    `contract_id` BIGINT NOT NULL COMMENT '合同ID（关联crm_contract）',
    `contract_no` VARCHAR(64) DEFAULT NULL COMMENT '合同编号（冗余）',
    `contract_dept_allocation_id` BIGINT DEFAULT NULL COMMENT '合同部门分配ID',
    
    -- 客户信息
    `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
    `customer_name` VARCHAR(255) DEFAULT NULL COMMENT '客户名称（冗余）',
    
    -- 驻场信息
    `name` VARCHAR(255) DEFAULT NULL COMMENT '项目名称',
    `onsite_location` VARCHAR(500) DEFAULT NULL COMMENT '驻场地点',
    `onsite_address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    `onsite_start_date` DATE DEFAULT NULL COMMENT '驻场开始日期',
    `onsite_end_date` DATE DEFAULT NULL COMMENT '驻场结束日期',
    
    -- 费用（两大块）
    `management_fee` DECIMAL(15,2) DEFAULT 0 COMMENT '管理费（元）',
    `onsite_fee` DECIMAL(15,2) DEFAULT 0 COMMENT '驻场费（元）',
    
    -- 负责人
    `manager_id` BIGINT DEFAULT NULL COMMENT '项目负责人ID',
    `manager_name` VARCHAR(64) DEFAULT NULL COMMENT '负责人姓名（冗余）',
    
    -- 人员统计
    `management_count` INT DEFAULT 0 COMMENT '管理人员数量',
    `onsite_count` INT DEFAULT 0 COMMENT '驻场人员数量',
    
    -- 状态
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待启动 1-进行中 2-已结束 3-已终止',
    
    -- 审计字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全运营合同表';


-- 2. 安全运营人员表
-- 记录管理人员和驻场人员
CREATE TABLE IF NOT EXISTS `security_operation_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `so_contract_id` BIGINT NOT NULL COMMENT '安全运营合同ID',
    
    -- 人员信息
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(64) DEFAULT NULL COMMENT '用户姓名（冗余）',
    
    -- 人员类型（关键！）
    `member_type` TINYINT NOT NULL COMMENT '人员类型：1-管理人员 2-驻场人员',
    
    -- 岗位信息
    `position_code` VARCHAR(64) DEFAULT NULL COMMENT '岗位代码',
    `position_name` VARCHAR(64) DEFAULT NULL COMMENT '岗位名称',
    
    -- 时间
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    
    -- 状态
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-待入场 1-在岗 2-已离开',
    
    -- 备注
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    
    -- 审计字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_so_contract_id` (`so_contract_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_member_type` (`member_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全运营人员表';


-- 3. 扩展服务项表，添加安全运营合同关联
ALTER TABLE `project_info` ADD COLUMN `so_contract_id` BIGINT DEFAULT NULL COMMENT '安全运营合同ID';
ALTER TABLE `project_info` ADD INDEX `idx_so_contract_id` (`so_contract_id`);
