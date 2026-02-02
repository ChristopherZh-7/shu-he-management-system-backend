-- 部门服务单表添加实际执行部门字段
-- 用于支持：根据负责人所在部门确定实际执行部门，在合同收入分配中显示子部门

-- 添加实际执行部门ID字段（根据负责人所在部门确定）
ALTER TABLE project_dept_service 
ADD COLUMN actual_dept_id BIGINT COMMENT '实际执行部门ID（根据负责人所在部门确定）';

-- 添加实际执行部门名称字段
ALTER TABLE project_dept_service 
ADD COLUMN actual_dept_name VARCHAR(255) COMMENT '实际执行部门名称';

-- 添加索引
ALTER TABLE project_dept_service ADD INDEX idx_dept_service_actual_dept (actual_dept_id);
