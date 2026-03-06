-- 1. crm_customer 增加客户类型字段
--    1 = 甲方客户（默认），2 = 集成商/合作伙伴
ALTER TABLE crm_customer
    ADD COLUMN `type` TINYINT NOT NULL DEFAULT 1 COMMENT '客户类型 1=甲方客户 2=集成商/合作伙伴';

-- 2. crm_business 增加集成商 ID
ALTER TABLE crm_business
    ADD COLUMN `intermediary_id` BIGINT NULL COMMENT '集成商客户ID，关联 crm_customer.id';

-- 3. crm_contract 增加集成商 ID
ALTER TABLE crm_contract
    ADD COLUMN `intermediary_id` BIGINT NULL COMMENT '集成商客户ID，关联 crm_customer.id';

-- 4. 隐藏线索管理菜单（ID: 2404 及其子菜单 2405-2409）
UPDATE system_menu SET `status` = 1 WHERE id IN (2404, 2405, 2406, 2407, 2408, 2409);
