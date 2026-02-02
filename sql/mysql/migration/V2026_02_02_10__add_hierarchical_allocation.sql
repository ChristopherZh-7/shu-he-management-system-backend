-- =====================================================
-- 合同收入分层分配改造
-- 支持逐级分配：合同 -> 一级部门 -> 二级部门 -> ... -> 班级
-- =====================================================

-- 1. 修改 contract_dept_allocation 表，增加分层分配字段
ALTER TABLE `contract_dept_allocation` 
ADD COLUMN `parent_allocation_id` bigint DEFAULT NULL COMMENT '上级分配ID（NULL表示从合同直接分配的第一级）' AFTER `dept_name`,
ADD COLUMN `allocation_level` int NOT NULL DEFAULT 1 COMMENT '分配层级（1=一级部门, 2=二级, 以此类推）' AFTER `parent_allocation_id`,
ADD COLUMN `received_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '从上级获得的金额' AFTER `allocation_level`,
ADD COLUMN `distributed_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '已分配给下级的金额' AFTER `received_amount`,
ADD INDEX `idx_parent_allocation_id` (`parent_allocation_id`);

-- 2. 迁移现有数据：将现有分配记录的 received_amount 设置为 allocated_amount
UPDATE `contract_dept_allocation` 
SET `received_amount` = `allocated_amount`,
    `distributed_amount` = 0,
    `allocation_level` = 1
WHERE `deleted` = 0;

-- 3. 添加外键约束（可选，建议在应用层控制）
-- ALTER TABLE `contract_dept_allocation`
-- ADD CONSTRAINT `fk_parent_allocation` 
-- FOREIGN KEY (`parent_allocation_id`) REFERENCES `contract_dept_allocation`(`id`);
