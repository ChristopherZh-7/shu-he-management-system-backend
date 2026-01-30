-- 允许security_operation_member表的so_contract_id字段为空
-- V2026_01_28_06
-- 因为驻场人员现在只需要关联到驻场点(site_id)，不一定需要关联到合同

ALTER TABLE `security_operation_member` MODIFY COLUMN `so_contract_id` BIGINT DEFAULT NULL COMMENT '安全运营合同ID（可选）';
