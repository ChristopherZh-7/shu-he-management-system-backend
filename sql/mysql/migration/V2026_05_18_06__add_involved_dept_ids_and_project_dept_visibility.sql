-- =====================================================
-- Migration: V2026_05_18_06__add_involved_dept_ids_and_project_dept_visibility.sql
-- Date:      2026-05-18
-- Description:
--   端到端实现「业界主流路径 2」：商机阶段就选「涉及部门」，提前投入/合同签订时
--   自动派生到 project_member + project_dept_visibility，部门下所有人都能看到项目。
--
--   背景：
--     之前 CRM 商机的 deptAllocations（部门金额分配）功能被业务方阉割，
--     无法用其派生「涉及部门 → 项目可见性」。需新增独立字段记录「涉及部门」
--     （只关注谁参与、不关心金额），用于派生项目成员 / 可见部门。
--
--   本 migration 做 3 件事：
--     1) crm_business 加 involved_dept_ids JSON 字段（[deptId, deptId, ...]）
--     2) project 加 involved_dept_ids JSON 字段（从商机派生过来）
--     3) 新表 project_dept_visibility（项目维度的部门可见性，部门下所有人可见）
--
--   设计原则：
--     - JSON 字段使用 MySQL 原生 JSON 类型（项目已用 8.0+）
--     - 新表用唯一索引 (project_id, dept_id) 防重
--     - 不修改任何已有数据，可幂等执行
-- =====================================================

-- ============================================================
-- 1. crm_business 加 involved_dept_ids JSON 字段
-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，用动态 SQL + INFORMATION_SCHEMA 判断
-- ============================================================
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'crm_business'
      AND COLUMN_NAME = 'involved_dept_ids'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE crm_business ADD COLUMN involved_dept_ids JSON DEFAULT NULL COMMENT ''涉及部门 id 数组（多选，用于派生项目可见性）''',
    'SELECT ''crm_business.involved_dept_ids already exists, skip'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. project 加 involved_dept_ids JSON 字段（同样判断）
-- ============================================================
SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'project'
      AND COLUMN_NAME = 'involved_dept_ids'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE project ADD COLUMN involved_dept_ids JSON DEFAULT NULL COMMENT ''涉及部门 id 数组（从商机派生而来，可在项目详情页修改）''',
    'SELECT ''project.involved_dept_ids already exists, skip'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 新表 project_dept_visibility
--    项目维度的部门可见性，部门下所有 user 都能看到该项目（getProjectPage UNION 进来）
-- ============================================================
CREATE TABLE IF NOT EXISTS project_dept_visibility (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id  BIGINT       NOT NULL COMMENT '项目 id',
    dept_id     BIGINT       NOT NULL COMMENT '部门 id',
    creator     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_dept (project_id, dept_id, deleted),
    KEY idx_dept_project (dept_id, project_id)
) COMMENT='项目维度的部门可见性·部门下所有人都能看到该项目';

-- ============================================================
-- 4. 验证
-- ============================================================
SELECT '===== Migration V2026_05_18_06 验证 =====' AS msg;

SELECT
    CONCAT('crm_business.involved_dept_ids 列存在 = ',
           (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'crm_business'
              AND COLUMN_NAME = 'involved_dept_ids')
    ) AS crm_business_col_check;

SELECT
    CONCAT('project.involved_dept_ids 列存在 = ',
           (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'project'
              AND COLUMN_NAME = 'involved_dept_ids')
    ) AS project_col_check;

SELECT
    CONCAT('project_dept_visibility 表存在 = ',
           (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'project_dept_visibility')
    ) AS table_check;

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- ALTER TABLE crm_business DROP COLUMN involved_dept_ids;
-- ALTER TABLE project DROP COLUMN involved_dept_ids;
-- DROP TABLE project_dept_visibility;
