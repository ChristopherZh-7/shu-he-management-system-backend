-- ==============================================
-- 项目驻场点模块 - 数据库迁移脚本
-- 
-- 功能：创建通用的项目驻场点和驻场人员表
-- 支持：安全服务、安全运营、数据安全等所有部门类型的项目
-- 
-- 执行顺序：
-- 1. 创建新表 project_site、project_site_member
-- 2. 从旧表 security_operation_site、security_operation_member 迁移数据
-- 3. 验证数据迁移完成后，可选择删除旧表
-- ==============================================

-- --------------------------------------------------
-- 1. 创建项目驻场点表
-- --------------------------------------------------
DROP TABLE IF EXISTS `project_site`;
CREATE TABLE `project_site` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `name` VARCHAR(100) DEFAULT NULL COMMENT '驻场点名称（如：客户总部、分公司A）',
    `address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `service_requirement` TEXT DEFAULT NULL COMMENT '服务要求（如：24小时值班、门禁管理）',
    `staff_count` INT DEFAULT 1 COMMENT '人员配置（需要驻场人数）',
    `start_date` DATE DEFAULT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目驻场点表';

-- --------------------------------------------------
-- 2. 创建项目驻场人员表
-- --------------------------------------------------
DROP TABLE IF EXISTS `project_site_member`;
CREATE TABLE `project_site_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `site_id` BIGINT NOT NULL COMMENT '驻场点ID',
    `project_id` BIGINT NOT NULL COMMENT '项目ID（冗余，便于查询）',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT '用户姓名（冗余）',
    `member_type` TINYINT DEFAULT 2 COMMENT '人员类型：1-管理人员 2-驻场人员',
    `is_leader` TINYINT DEFAULT 0 COMMENT '是否项目负责人：0-否 1-是',
    `position_code` VARCHAR(50) DEFAULT NULL COMMENT '岗位代码',
    `position_name` VARCHAR(50) DEFAULT NULL COMMENT '岗位名称',
    `start_date` DATE DEFAULT NULL COMMENT '入场日期',
    `end_date` DATE DEFAULT NULL COMMENT '离开日期',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-待入场 1-在岗 2-已离开',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_site_id` (`site_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目驻场人员表';

-- --------------------------------------------------
-- 3. 数据迁移：从 security_operation_site 迁移到 project_site
-- --------------------------------------------------
INSERT INTO `project_site` (
    `id`, `project_id`, `name`, `address`, `contact_name`, `contact_phone`,
    `service_requirement`, `staff_count`, `start_date`, `end_date`,
    `status`, `remark`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 
    `id`, `project_id`, `name`, `address`, `contact_name`, `contact_phone`,
    `service_requirement`, `staff_count`, `start_date`, `end_date`,
    `status`, `remark`, `sort`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
FROM `security_operation_site`
WHERE `deleted` = 0;

-- --------------------------------------------------
-- 4. 数据迁移：从 security_operation_member 迁移到 project_site_member
-- --------------------------------------------------
INSERT INTO `project_site_member` (
    `id`, `site_id`, `project_id`, `user_id`, `user_name`,
    `member_type`, `is_leader`, `position_code`, `position_name`,
    `start_date`, `end_date`, `status`, `remark`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`
)
SELECT 
    som.`id`, 
    som.`site_id`,
    sos.`project_id`,  -- 从驻场点获取 project_id
    som.`user_id`, 
    som.`user_name`,
    som.`member_type`, 
    som.`is_leader`, 
    som.`position_code`, 
    som.`position_name`,
    som.`start_date`, 
    som.`end_date`, 
    som.`status`, 
    som.`remark`,
    som.`creator`, 
    som.`create_time`, 
    som.`updater`, 
    som.`update_time`, 
    som.`deleted`, 
    som.`tenant_id`
FROM `security_operation_member` som
LEFT JOIN `security_operation_site` sos ON som.`site_id` = sos.`id`
WHERE som.`deleted` = 0;

-- --------------------------------------------------
-- 5. 验证迁移数据（可选，执行后检查结果）
-- --------------------------------------------------
-- SELECT '旧表 security_operation_site 数量' AS description, COUNT(*) AS count FROM security_operation_site WHERE deleted = 0
-- UNION ALL
-- SELECT '新表 project_site 数量' AS description, COUNT(*) AS count FROM project_site WHERE deleted = 0
-- UNION ALL
-- SELECT '旧表 security_operation_member 数量' AS description, COUNT(*) AS count FROM security_operation_member WHERE deleted = 0
-- UNION ALL
-- SELECT '新表 project_site_member 数量' AS description, COUNT(*) AS count FROM project_site_member WHERE deleted = 0;

-- --------------------------------------------------
-- 6. 清理旧表（确认数据迁移完成后再执行）
-- --------------------------------------------------
-- 警告：以下操作会删除旧表，请在确认数据迁移正确后再执行！
-- DROP TABLE IF EXISTS `security_operation_site`;
-- DROP TABLE IF EXISTS `security_operation_member`;
