-- 移除项目管理中的预算/金额字段，财务管理将由独立模块处理

-- project_dept_service: 移除部门预算字段
ALTER TABLE project_dept_service
    DROP COLUMN dept_budget,
    DROP COLUMN onsite_budget,
    DROP COLUMN second_line_budget;

-- project_info (service items): 移除资金分配字段
ALTER TABLE project_info DROP COLUMN allocated_amount;
