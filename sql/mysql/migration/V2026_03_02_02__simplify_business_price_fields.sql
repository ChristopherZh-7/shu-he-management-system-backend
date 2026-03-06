-- =============================================
-- 商机重构：
-- 1. 新增钉钉群会话ID字段
-- 2. 删除 crm_business_product 表（移除产品清单功能）
-- 注意：dept_ids 和金额字段已在之前手动处理
-- =============================================

-- 1. 新增钉钉群会话ID字段
ALTER TABLE crm_business ADD COLUMN dingtalk_chat_id varchar(128) DEFAULT NULL COMMENT '钉钉群会话ID';

-- 2. 删除商机产品关联表
DROP TABLE IF EXISTS crm_business_product;
