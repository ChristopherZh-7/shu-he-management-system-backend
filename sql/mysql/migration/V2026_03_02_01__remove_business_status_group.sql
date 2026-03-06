-- =============================================
-- 移除商机状态组功能
-- 1. 删除 crm_business 表中的 status_type_id 和 status_id 字段
-- 2. 删除 crm_business_status 和 crm_business_status_type 表
-- 3. 删除商机状态配置相关菜单
-- =============================================

-- 1. 删除 crm_business 表中的状态组相关字段
ALTER TABLE crm_business DROP COLUMN status_type_id, DROP COLUMN status_id;

-- 2. 删除商机状态相关表
DROP TABLE IF EXISTS crm_business_status;
DROP TABLE IF EXISTS crm_business_status_type;

-- 3. 删除商机状态配置相关菜单（id: 2703-2707）
UPDATE system_menu SET deleted = 1 WHERE id IN (2703, 2704, 2705, 2706, 2707);
