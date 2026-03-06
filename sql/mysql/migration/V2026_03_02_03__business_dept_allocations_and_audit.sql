-- =============================================
-- 商机审批 + 部门金额分配
-- 1. dept_ids 列改为 dept_allocations（JSON 格式，含部门名+金额）
-- 2. 新增 audit_status 审批状态
-- 3. 新增 process_instance_id BPM流程实例ID
-- =============================================

-- 1. 将 dept_ids 列改名为 dept_allocations
ALTER TABLE crm_business CHANGE COLUMN dept_ids dept_allocations varchar(2048) DEFAULT NULL COMMENT '部门金额分配(JSON)';

-- 2. 新增审批状态字段
ALTER TABLE crm_business ADD COLUMN audit_status tinyint DEFAULT 0 COMMENT '审批状态(0-未提交,10-审核中,20-通过,30-不通过,40-取消)';

-- 3. 新增BPM流程实例ID
ALTER TABLE crm_business ADD COLUMN process_instance_id varchar(64) DEFAULT NULL COMMENT 'BPM流程实例ID';
