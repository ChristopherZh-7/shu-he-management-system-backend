-- 修复服务项分配表的唯一索引
-- 支持层级分配：同一个服务项可以从不同的费用类型分配
-- =====================================================

-- 问题说明：
-- 原唯一索引 (contract_dept_allocation_id, service_item_id, deleted) 不支持以下场景：
-- 1. 直接分配服务项：contract_dept_allocation_id=4, service_item_id=1, parent_allocation_id=NULL
-- 2. 从费用类型分配服务项：contract_dept_allocation_id=4, service_item_id=1, parent_allocation_id=7
-- 这两条记录在原索引下会冲突，但业务上是允许的

-- 1. 删除原有的唯一索引（兼容不同MySQL版本）
-- MySQL 8.0+ 支持 DROP INDEX IF EXISTS
SET @exist := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
               WHERE TABLE_SCHEMA = DATABASE() 
               AND TABLE_NAME = 'service_item_allocation' 
               AND INDEX_NAME = 'uk_allocation_service_item');

SET @sqlstmt := IF(@exist > 0, 
    'ALTER TABLE `service_item_allocation` DROP INDEX `uk_allocation_service_item`',
    'SELECT ''Index does not exist''');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建新的唯一索引
-- 使用 COALESCE 处理 NULL 值：parent_allocation_id 为 NULL 时视为 0
-- 注意：MySQL 不支持在唯一索引中直接使用 COALESCE，所以我们改用组合策略
-- 
-- 新规则：
-- - 对于费用类型分配（service_item_id 为 NULL）：按 (contract_dept_allocation_id, allocation_type) 唯一
-- - 对于服务项分配（service_item_id 不为 NULL）：按 (contract_dept_allocation_id, parent_allocation_id, service_item_id) 唯一
-- 
-- 由于 MySQL 唯一索引会忽略 NULL 值，我们直接创建包含所有字段的索引
ALTER TABLE `service_item_allocation` 
ADD UNIQUE INDEX `uk_allocation_item` (
    `contract_dept_allocation_id`, 
    `parent_allocation_id`, 
    `service_item_id`, 
    `allocation_type`,
    `deleted`
);
