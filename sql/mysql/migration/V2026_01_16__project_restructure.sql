-- =====================================================
-- 项目管理模块结构重构迁移脚本
-- 变更: 新增项目表作为第一层，原 project_info 表改为服务项
-- 日期: 2026-01-16
-- =====================================================

-- 1. 创建新的项目表（顶层项目）
CREATE TABLE IF NOT EXISTS `project` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `name` VARCHAR(255) NOT NULL COMMENT '项目名称',
    `code` VARCHAR(100) DEFAULT NULL COMMENT '项目编号',
    `dept_type` INT NOT NULL COMMENT '部门类型：1安全服务 2安全运营 3数据安全',
    `customer_id` BIGINT DEFAULT NULL COMMENT 'CRM客户ID',
    `customer_name` VARCHAR(255) DEFAULT NULL COMMENT '客户名称',
    `contract_id` BIGINT DEFAULT NULL COMMENT 'CRM合同ID',
    `contract_no` VARCHAR(100) DEFAULT NULL COMMENT '合同编号',
    `status` INT DEFAULT 0 COMMENT '状态：0草稿 1进行中 2已完成',
    `description` TEXT DEFAULT NULL COMMENT '项目描述',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    INDEX `idx_dept_type` (`dept_type`),
    INDEX `idx_customer_id` (`customer_id`),
    INDEX `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- 2. 为原 project_info 表添加 project_id 外键字段（关联到新项目表）
ALTER TABLE `project_info` 
    ADD COLUMN `project_id` BIGINT DEFAULT NULL COMMENT '所属项目ID' AFTER `id`;

-- 3. 添加索引
ALTER TABLE `project_info` 
    ADD INDEX `idx_project_id` (`project_id`);

-- 4. 数据迁移：为现有的服务项创建对应的项目记录
-- 按 (customer_id, contract_id, dept_type) 分组创建项目
INSERT INTO `project` (`name`, `code`, `dept_type`, `customer_id`, `customer_name`, `contract_id`, `contract_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 
    CONCAT(COALESCE(pi.customer_name, '未知客户'), '-项目') AS `name`,
    CONCAT('PRJ-', pi.dept_type, '-', DATE_FORMAT(pi.create_time, '%Y%m%d'), '-', LPAD(pi.id, 4, '0')) AS `code`,
    pi.dept_type,
    pi.customer_id,
    pi.customer_name,
    pi.contract_id,
    pi.contract_no,
    CASE WHEN pi.status = 3 THEN 2 WHEN pi.status IN (1, 2) THEN 1 ELSE 0 END AS `status`,
    pi.creator,
    MIN(pi.create_time),
    pi.updater,
    MAX(pi.update_time),
    0,
    pi.tenant_id
FROM `project_info` pi
WHERE pi.deleted = 0
GROUP BY pi.customer_id, pi.contract_id, pi.dept_type, pi.customer_name, pi.contract_no, pi.creator, pi.updater, pi.tenant_id;

-- 5. 更新 project_info 表的 project_id 关联
UPDATE `project_info` pi
INNER JOIN `project` p ON (
    (pi.customer_id = p.customer_id OR (pi.customer_id IS NULL AND p.customer_id IS NULL))
    AND (pi.contract_id = p.contract_id OR (pi.contract_id IS NULL AND p.contract_id IS NULL))
    AND pi.dept_type = p.dept_type
)
SET pi.project_id = p.id
WHERE pi.deleted = 0;

-- 6. 更新表注释（可选：将 project_info 重命名为 service_item）
-- 如果你想重命名表，取消下面的注释：
-- RENAME TABLE `project_info` TO `service_item`;

-- 注意：如果不重命名表，则在Java代码中使用 @TableName("project_info") 保持映射
