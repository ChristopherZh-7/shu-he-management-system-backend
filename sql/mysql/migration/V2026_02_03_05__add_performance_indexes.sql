-- ============================================
-- 性能优化：添加数据库索引
-- 用于加速经营分析和工作台数据查询
-- ============================================

-- 1. outside_cost_record 表索引优化
-- 用于跨部门费用查询

-- 检查并添加 request_dept_id + fill_time 复合索引（用于部门支出统计）
SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'outside_cost_record' 
    AND index_name = 'idx_request_dept_fill_time');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_request_dept_fill_time ON outside_cost_record(request_dept_id, fill_time, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查并添加 target_dept_id + fill_time 复合索引（用于部门收入统计）
SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'outside_cost_record' 
    AND index_name = 'idx_target_dept_fill_time');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_target_dept_fill_time ON outside_cost_record(target_dept_id, fill_time, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. project_site_member 表索引优化
-- 用于驻场参与查询

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'project_site_member' 
    AND index_name = 'idx_user_dept_type');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_user_dept_type ON project_site_member(user_id, dept_type, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. project_round 表索引优化
-- 用于轮次执行查询

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'project_round' 
    AND index_name = 'idx_status_actual_end');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_status_actual_end ON project_round(status, actual_end_time, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. security_operation_member 表索引优化

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'security_operation_member' 
    AND index_name = 'idx_user_status');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_user_status ON security_operation_member(user_id, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. service_item_allocation 表索引优化

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'service_item_allocation' 
    AND index_name = 'idx_contract_dept_type');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_contract_dept_type ON service_item_allocation(contract_dept_allocation_id, allocation_type, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. contract_dept_allocation 表索引优化

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'contract_dept_allocation' 
    AND index_name = 'idx_contract_dept');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_contract_dept ON contract_dept_allocation(contract_id, dept_id, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. system_users 表索引优化（如果不存在）

SET @index_exists = (SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS 
    WHERE table_schema = DATABASE() AND table_name = 'system_users' 
    AND index_name = 'idx_dept_status');
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_dept_status ON system_users(dept_id, status, deleted)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
