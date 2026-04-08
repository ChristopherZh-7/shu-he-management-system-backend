-- 财务管理模块：重新设计为统一的层级分配表

-- 删除旧表
DROP TABLE IF EXISTS finance_service_allocation;
DROP TABLE IF EXISTS finance_project_budget;

-- 统一的财务分配表：支持多级层级分配（合同→部门→预算池→服务项）
CREATE TABLE `finance_allocation` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`           BIGINT        NULL     COMMENT '上级分配ID（NULL=合同级别的第一层分配）',
    `contract_id`         BIGINT        NOT NULL COMMENT '关联的CRM合同ID',
    `dept_service_id`     BIGINT        NULL     COMMENT '关联的部门服务单ID（level 2/3）',
    `service_item_id`     BIGINT        NULL     COMMENT '关联的服务项ID（仅level 3）',
    `allocation_level`    TINYINT       NOT NULL COMMENT '分配层级：1=合同→部门 2=部门→预算池 3=预算池→服务项',
    `allocation_type`     VARCHAR(32)   NOT NULL COMMENT '分配类型：dept/onsite_pool/second_line_pool/service_item',
    `dept_id`             BIGINT        NULL     COMMENT '部门ID',
    `dept_name`           VARCHAR(100)  NULL     COMMENT '部门名称（冗余）',
    `dept_type`           TINYINT       NULL     COMMENT '部门类型：1-安全服务 2-安全运营 3-数据安全',
    `name`                VARCHAR(200)  NULL     COMMENT '分配名称（如：部门名称、驻场池、二线池、服务项名称）',
    `allocated_amount`    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '分配金额',
    `remark`              VARCHAR(500)  NULL     COMMENT '备注',
    `creator`             VARCHAR(64)   NULL DEFAULT '' COMMENT '创建者',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`             VARCHAR(64)   NULL DEFAULT '' COMMENT '更新者',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`           BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_dept_service_id` (`dept_service_id`),
    KEY `idx_service_item_id` (`service_item_id`),
    KEY `idx_allocation_level` (`allocation_level`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务分配表（层级结构）';
