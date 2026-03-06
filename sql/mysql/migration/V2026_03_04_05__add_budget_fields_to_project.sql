-- ============================================================
-- 资金池重构：将费用池从合同收入分配迁移至项目管理流程字段
-- ============================================================

-- 1. project_dept_service 新增预算字段
--    dept_budget:       合同分配给本部门的总预算（领取时从 crm_contract.dept_allocations 带入）
--    onsite_budget:     驻场费预算（设置负责人时填写）
--    second_line_budget:二线/管理预算（设置负责人时填写）
ALTER TABLE project_dept_service
    ADD COLUMN dept_budget        DECIMAL(15, 2) NULL COMMENT '合同分配给本部门的总预算' AFTER remark,
    ADD COLUMN onsite_budget      DECIMAL(15, 2) NULL COMMENT '驻场费预算（设置负责人时填写）' AFTER dept_budget,
    ADD COLUMN second_line_budget DECIMAL(15, 2) NULL COMMENT '二线/管理预算（设置负责人时填写）' AFTER onsite_budget;

-- 2. project_info 新增服务项分配字段
--    allocated_amount:  分配给该服务项的金额（二线/管理服务项创建时填写）
--    executor_id:       执行人用户ID（二线服务项指定，驻场服务项不需要）
--    executor_name:     执行人姓名（冗余存储，避免关联查询）
ALTER TABLE project_info
    ADD COLUMN allocated_amount DECIMAL(15, 2) NULL COMMENT '分配给该服务项的金额' AFTER remark,
    ADD COLUMN executor_id      BIGINT         NULL COMMENT '执行人用户ID（二线/管理服务项）' AFTER allocated_amount,
    ADD COLUMN executor_name    VARCHAR(64)    NULL COMMENT '执行人姓名（冗余存储）' AFTER executor_id;

-- 3. 更新 v_contract_allocation_fees 视图
--    旧：从 service_item_allocation 汇总
--    新：从 project_dept_service 的新字段读取
DROP VIEW IF EXISTS v_contract_allocation_fees;

CREATE VIEW v_contract_allocation_fees AS
SELECT p.contract_id,
       pds.dept_type,
       COALESCE(SUM(pds.onsite_budget), 0)       AS total_onsite_fee,
       COALESCE(SUM(pds.second_line_budget), 0)  AS total_management_fee
FROM project_dept_service pds
         JOIN project p ON p.id = pds.project_id AND p.deleted = 0
WHERE pds.deleted = 0
GROUP BY p.contract_id, pds.dept_type;
