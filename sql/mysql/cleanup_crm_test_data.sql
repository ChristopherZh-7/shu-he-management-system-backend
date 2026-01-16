-- =============================================
-- CRM 测试数据清理脚本
-- 执行时间: 2026-01-15
-- 注意: 此脚本会删除 CRM 模块的所有业务数据，请谨慎执行！
-- =============================================

-- 1. 删除回款相关数据
DELETE FROM crm_receivable WHERE deleted = 0;
DELETE FROM crm_receivable_plan WHERE deleted = 0;

-- 2. 删除合同相关数据
DELETE FROM crm_contract WHERE deleted = 0;
DELETE FROM crm_contract_product WHERE deleted = 0;

-- 3. 删除商机相关数据
DELETE FROM crm_business WHERE deleted = 0;
DELETE FROM crm_business_product WHERE deleted = 0;
DELETE FROM crm_business_status WHERE deleted = 0;
DELETE FROM crm_business_status_type WHERE deleted = 0;

-- 4. 删除联系人相关数据
DELETE FROM crm_contact WHERE deleted = 0;
DELETE FROM crm_contact_business WHERE deleted = 0;

-- 5. 删除客户相关数据
DELETE FROM crm_customer WHERE deleted = 0;
DELETE FROM crm_customer_pool_config WHERE deleted = 0;
DELETE FROM crm_customer_limit_config WHERE deleted = 0;

-- 6. 删除线索数据
DELETE FROM crm_clue WHERE deleted = 0;

-- 7. 删除产品数据
DELETE FROM crm_product WHERE deleted = 0;
DELETE FROM crm_product_category WHERE deleted = 0;

-- 8. 删除跟进记录
DELETE FROM crm_follow_up_record WHERE deleted = 0;

-- 9. 删除 CRM 权限数据
DELETE FROM crm_permission WHERE deleted = 0;

-- 10. 删除 CRM 相关的 BPM 流程实例（如果有的话）
-- 注意：这里只是标记删除，实际的 Flowable 数据需要单独清理
-- DELETE FROM bpm_process_instance WHERE business_key LIKE 'crm_%';

SELECT '✅ CRM 测试数据清理完成！' AS result;
