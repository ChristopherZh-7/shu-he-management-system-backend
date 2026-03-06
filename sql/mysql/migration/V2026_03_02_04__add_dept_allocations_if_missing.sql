-- =============================================
-- 补充 dept_allocations 列（当 V2026_03_02_03 因 dept_ids 不存在而失败时）
-- 若列已存在则跳过
-- =============================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_business' AND COLUMN_NAME = 'dept_allocations'
);

SET @dept_ids_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_business' AND COLUMN_NAME = 'dept_ids'
);

SET @stmt = IF(@col_exists > 0, 'SELECT 1',
    IF(@dept_ids_exists > 0,
        'ALTER TABLE crm_business CHANGE COLUMN dept_ids dept_allocations varchar(2048) DEFAULT NULL COMMENT ''部门金额分配(JSON)''',
        'ALTER TABLE crm_business ADD COLUMN dept_allocations varchar(2048) DEFAULT NULL COMMENT ''部门金额分配(JSON)'''
    )
);

PREPARE s FROM @stmt;
EXECUTE s;
DEALLOCATE PREPARE s;
