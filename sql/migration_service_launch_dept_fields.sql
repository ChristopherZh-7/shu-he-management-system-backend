-- 服务发起表添加审批人和实际执行部门相关字段
-- 用于支持：叶子部门选择、审批人向上递归查找、审批时选择子部门执行

-- 添加审批人所在部门ID字段
ALTER TABLE project_service_launch 
ADD COLUMN approver_dept_id BIGINT COMMENT '审批人所在部门ID（如果用户选择的部门没有负责人，会向上递归查找）';

-- 添加是否需要在审批时选择执行的子部门字段
ALTER TABLE project_service_launch 
ADD COLUMN need_select_execute_dept TINYINT(1) DEFAULT 0 COMMENT '是否需要在审批时选择执行的子部门（当审批人是父部门负责人时为true）';

-- 添加实际执行部门ID字段（审批时选择的子部门）
ALTER TABLE project_service_launch 
ADD COLUMN actual_execute_dept_id BIGINT COMMENT '实际执行部门ID（审批时选择的子部门，如果不需要选择子部门则与execute_dept_id相同）';

-- 添加索引
ALTER TABLE project_service_launch ADD INDEX idx_service_launch_approver_dept (approver_dept_id);
ALTER TABLE project_service_launch ADD INDEX idx_service_launch_actual_execute_dept (actual_execute_dept_id);
