-- 扩大合同金额字段精度
-- 原来是 decimal(10,2)，最大值约 1亿
-- 扩大到 decimal(18,2)，最大值约 1亿亿

ALTER TABLE `crm_contract` 
MODIFY COLUMN `total_price` decimal(18,2) DEFAULT NULL COMMENT '合同总金额';

ALTER TABLE `crm_contract` 
MODIFY COLUMN `total_product_price` decimal(18,2) DEFAULT NULL COMMENT '产品总金额';
