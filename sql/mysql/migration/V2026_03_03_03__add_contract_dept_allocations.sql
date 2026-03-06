-- 合同表增加部门金额分配字段
-- 签合同时可以重新按部门分配合同金额（与商机的 dept_allocations 格式相同）
ALTER TABLE crm_contract
    ADD COLUMN `dept_allocations` JSON NULL COMMENT '部门金额分配 JSON，格式同 crm_business.dept_allocations';
