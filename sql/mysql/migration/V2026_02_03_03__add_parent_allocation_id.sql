-- 为服务项分配表添加父级分配ID字段
-- 用于支持三级分配结构：合同 → 部门 → 费用类型 → 具体服务项
-- 当从费用类型分配（如"二线服务费"）再分配到具体服务项时，需要记录父级关系

-- 1. 添加 parent_allocation_id 字段
ALTER TABLE `service_item_allocation` ADD COLUMN `parent_allocation_id` BIGINT DEFAULT NULL 
COMMENT '父级分配ID（用于层级分配，指向费用类型分配记录）' AFTER `contract_dept_allocation_id`;

-- 2. 添加索引以提高查询效率
ALTER TABLE `service_item_allocation` ADD INDEX `idx_parent_allocation_id` (`parent_allocation_id`);

-- 3. 添加外键约束（可选，确保数据完整性）
-- ALTER TABLE `service_item_allocation` ADD CONSTRAINT `fk_parent_allocation` 
-- FOREIGN KEY (`parent_allocation_id`) REFERENCES `service_item_allocation` (`id`) ON DELETE SET NULL;
