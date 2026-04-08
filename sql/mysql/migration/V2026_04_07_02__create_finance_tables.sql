-- 财务管理模块：创建项目预算和服务项收入分配表

-- 项目预算表：管理每个部门服务单的预算分配
CREATE TABLE IF NOT EXISTS `finance_project_budget` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_service_id`   BIGINT       NOT NULL COMMENT '关联的部门服务单ID（project_dept_service.id）',
    `project_id`        BIGINT       NULL     COMMENT '项目ID（冗余）',
    `contract_id`       BIGINT       NULL     COMMENT '合同ID（冗余）',
    `dept_id`           BIGINT       NULL     COMMENT '部门ID（冗余）',
    `dept_name`         VARCHAR(100) NULL     COMMENT '部门名称（冗余）',
    `dept_type`         TINYINT      NOT NULL COMMENT '部门类型：1-安全服务 2-安全运营 3-数据安全',
    `dept_budget`       DECIMAL(12,2) NULL    COMMENT '部门总预算',
    `onsite_budget`     DECIMAL(12,2) NULL    COMMENT '驻场预算',
    `second_line_budget` DECIMAL(12,2) NULL   COMMENT '二线预算',
    `remark`            VARCHAR(500) NULL     COMMENT '备注',
    `creator`           VARCHAR(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           VARCHAR(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_service_id` (`dept_service_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目预算表';

-- 服务项收入分配表：记录每个服务项的收入分配金额
CREATE TABLE IF NOT EXISTS `finance_service_allocation` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `service_item_id`     BIGINT        NOT NULL COMMENT '关联的服务项ID（project_info.id）',
    `budget_id`           BIGINT        NULL     COMMENT '关联的项目预算ID（finance_project_budget.id）',
    `dept_service_id`     BIGINT        NULL     COMMENT '部门服务单ID（冗余）',
    `allocated_amount`    DECIMAL(12,2) NOT NULL COMMENT '分配金额',
    `service_mode`        TINYINT       NULL     COMMENT '服务模式：1-驻场 2-二线',
    `service_member_type` TINYINT       NULL     COMMENT '服务归属人员类型：1-驻场人员 2-管理人员',
    `remark`              VARCHAR(500)  NULL     COMMENT '备注',
    `creator`             VARCHAR(64)   NULL DEFAULT '' COMMENT '创建者',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`             VARCHAR(64)   NULL DEFAULT '' COMMENT '更新者',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`           BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_service_item_id` (`service_item_id`),
    KEY `idx_budget_id` (`budget_id`),
    KEY `idx_dept_service_id` (`dept_service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项收入分配表';
