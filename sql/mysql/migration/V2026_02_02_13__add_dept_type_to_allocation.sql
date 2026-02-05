-- =====================================================
-- 为合同部门分配表添加部门类型字段
-- 用于区分安全运营(2)、安全服务(1)、数据安全(3)等不同类型的分配
-- =====================================================

-- 1. 添加 dept_type 字段到 contract_dept_allocation 表
ALTER TABLE `contract_dept_allocation` 
ADD COLUMN `dept_type` tinyint DEFAULT NULL COMMENT '部门类型：1-安全服务 2-安全运营 3-数据安全' AFTER `dept_name`;

-- 2. 添加索引以优化查询性能
ALTER TABLE `contract_dept_allocation`
ADD INDEX `idx_dept_type` (`dept_type`);

-- 3. 更新现有数据的 dept_type（根据 dept_id 从 system_dept 获取）
-- 注意：这里假设 system_dept 表有 dept_type 字段，如果没有则需要手动设置
UPDATE `contract_dept_allocation` cda
LEFT JOIN `system_dept` sd ON cda.dept_id = sd.id
SET cda.dept_type = sd.dept_type
WHERE cda.deleted = 0 AND sd.dept_type IS NOT NULL;
