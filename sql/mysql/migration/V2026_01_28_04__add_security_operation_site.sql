-- 安全运营驻场点表
-- 一个项目可以有多个驻场点

CREATE TABLE IF NOT EXISTS `security_operation_site` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 关联项目
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    
    -- 驻场点基本信息
    `name` VARCHAR(255) NOT NULL COMMENT '驻场点名称（如：客户总部、分公司A）',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    
    -- 联系信息
    `contact_name` VARCHAR(64) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    
    -- 服务配置
    `service_requirement` VARCHAR(1000) DEFAULT NULL COMMENT '服务要求（如：24小时值班、门禁管理）',
    `staff_count` INT DEFAULT 1 COMMENT '人员配置（需要驻场人数）',
    
    -- 时间
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    
    -- 状态
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    
    -- 备注
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    
    -- 排序
    `sort` INT DEFAULT 0 COMMENT '排序',
    
    -- 审计字段
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全运营驻场点表';
