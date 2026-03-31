-- =============================================================================
-- DANGEROUS: 清空客户模块列表数据（客户 + 联系人 + 关联）
-- 保留：crm_contract_config、crm_product、crm_customer_pool_config 等配置/主数据
-- 不删：crm_clue（线索公海，与客户列表独立；若也要清空线索请单独执行 TRUNCATE crm_clue）
-- =============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE crm_contact_business;
TRUNCATE TABLE crm_contact;

DELETE FROM crm_permission WHERE biz_type IN (2, 3);

TRUNCATE TABLE crm_follow_up_record;

TRUNCATE TABLE crm_customer;

SET FOREIGN_KEY_CHECKS = 1;
