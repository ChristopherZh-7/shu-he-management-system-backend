-- 商机审批通过后直接创建项目，不再从合同触发
-- 1. project 表增加商机关联字段
ALTER TABLE project
    ADD COLUMN `business_id` BIGINT NULL COMMENT 'CRM商机ID，关联 crm_business.id';

-- 2. project_dept_service 表增加商机关联字段
ALTER TABLE project_dept_service
    ADD COLUMN `business_id` BIGINT NULL COMMENT 'CRM商机ID，关联 crm_business.id';
