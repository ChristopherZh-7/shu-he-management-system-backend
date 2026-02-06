-- ============================================
-- 性能优化：project_site_member 查询优化
-- 解决经营分析和工作台页面慢查询问题
-- ============================================

-- 1. 添加 project_site_member 表的复合索引
-- 用于优化 sameMemberTypeCount 子查询

-- 索引用于按 site_id + member_type 统计
SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'project_site_member' 
    AND index_name = 'idx_site_member_type_count');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_site_member_type_count ON project_site_member(site_id, member_type, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加 contract_dept_allocation 表优化索引
-- 用于优化 onsiteFee 和 managementFee 子查询

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'contract_dept_allocation' 
    AND index_name = 'idx_contract_dept_type');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_contract_dept_type ON contract_dept_allocation(contract_id, dept_type, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 添加 service_item_allocation 表优化索引

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'service_item_allocation' 
    AND index_name = 'idx_allocation_type_amount');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_allocation_type_amount ON service_item_allocation(contract_dept_allocation_id, allocation_type, deleted, allocated_amount)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 创建费用汇总视图（可选，用于简化查询）
-- 注意：视图在每次查询时计算，如果数据量大可能仍然慢
-- 如果需要更好的性能，建议使用定时任务预计算或Redis缓存

DROP VIEW IF EXISTS v_contract_allocation_fees;

CREATE VIEW v_contract_allocation_fees AS
SELECT 
    cda.contract_id,
    cda.dept_type,
    SUM(CASE 
        WHEN sia.allocation_type IN ('so_onsite', 'ss_onsite', 'ds_onsite', 'service_onsite') 
        THEN sia.allocated_amount ELSE 0 
    END) as total_onsite_fee,
    SUM(CASE 
        WHEN sia.allocation_type = 'so_management' 
        THEN sia.allocated_amount ELSE 0 
    END) as total_management_fee
FROM contract_dept_allocation cda
LEFT JOIN service_item_allocation sia 
    ON sia.contract_dept_allocation_id = cda.id 
    AND sia.deleted = 0
WHERE cda.deleted = 0
GROUP BY cda.contract_id, cda.dept_type;

-- 5. 创建驻场点成员数统计视图

DROP VIEW IF EXISTS v_site_member_type_count;

CREATE VIEW v_site_member_type_count AS
SELECT 
    site_id,
    member_type,
    COUNT(*) as member_count
FROM project_site_member
WHERE deleted = 0 AND status = 1
GROUP BY site_id, member_type;

-- 6. 创建安全运营合同成员数统计视图
-- 用于优化 SecurityOperationContractInfoMapper 的 sameMemberTypeCount 子查询

DROP VIEW IF EXISTS v_so_contract_member_type_count;

CREATE VIEW v_so_contract_member_type_count AS
SELECT 
    so_contract_id,
    member_type,
    COUNT(*) as member_count
FROM security_operation_member
WHERE deleted = 0 AND status = 1
GROUP BY so_contract_id, member_type;

-- 7. 创建安全运营合同费用视图
-- 用于优化 SecurityOperationContractInfoMapper 的费用子查询

DROP VIEW IF EXISTS v_so_contract_fees;

CREATE VIEW v_so_contract_fees AS
SELECT 
    soc.id as so_contract_id,
    soc.contract_dept_allocation_id,
    COALESCE(SUM(CASE WHEN sia.allocation_type = 'so_management' THEN sia.allocated_amount ELSE 0 END), 0) as management_fee,
    COALESCE(SUM(CASE WHEN sia.allocation_type = 'so_onsite' THEN sia.allocated_amount ELSE 0 END), 0) as onsite_fee
FROM security_operation_contract soc
LEFT JOIN service_item_allocation sia 
    ON sia.contract_dept_allocation_id = soc.contract_dept_allocation_id 
    AND sia.deleted = 0
WHERE soc.deleted = 0
GROUP BY soc.id, soc.contract_dept_allocation_id;

-- 8. 添加 security_operation_member 表的复合索引

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'security_operation_member' 
    AND index_name = 'idx_so_contract_member_type');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_so_contract_member_type ON security_operation_member(so_contract_id, member_type, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
