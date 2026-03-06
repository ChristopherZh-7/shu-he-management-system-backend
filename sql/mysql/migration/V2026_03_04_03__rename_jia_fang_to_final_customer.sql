-- 将数据库列注释中的"甲方客户"→"最终客户"，"集成商"→"合作商"
-- 注：只更新注释，不修改列名称和数据，不影响现有数据

-- 1. crm_customer.type 注释更新
ALTER TABLE crm_customer
    MODIFY COLUMN `type` TINYINT NOT NULL DEFAULT 1
        COMMENT '客户类型 1=最终客户 2=合作商';

-- 2. crm_business.intermediary_id 注释更新
ALTER TABLE crm_business
    MODIFY COLUMN `intermediary_id` BIGINT NULL
        COMMENT '合作商客户ID，关联 crm_customer.id';

-- 3. crm_contract.intermediary_id 注释更新
ALTER TABLE crm_contract
    MODIFY COLUMN `intermediary_id` BIGINT NULL
        COMMENT '合作商客户ID，关联 crm_customer.id';
