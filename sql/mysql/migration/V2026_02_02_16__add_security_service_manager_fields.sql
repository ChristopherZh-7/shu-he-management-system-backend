-- 为 project_dept_service 表添加安全服务专用的驻场和二线负责人字段

-- 添加驻场负责人ID列表字段
ALTER TABLE project_dept_service 
ADD COLUMN onsite_manager_ids VARCHAR(500) DEFAULT NULL COMMENT '驻场负责人ID列表（JSON数组，仅安全服务使用）' AFTER manager_names;

-- 添加驻场负责人姓名列表字段
ALTER TABLE project_dept_service 
ADD COLUMN onsite_manager_names VARCHAR(500) DEFAULT NULL COMMENT '驻场负责人姓名列表（JSON数组，仅安全服务使用）' AFTER onsite_manager_ids;

-- 添加二线负责人ID列表字段
ALTER TABLE project_dept_service 
ADD COLUMN second_line_manager_ids VARCHAR(500) DEFAULT NULL COMMENT '二线负责人ID列表（JSON数组，仅安全服务使用）' AFTER onsite_manager_names;

-- 添加二线负责人姓名列表字段
ALTER TABLE project_dept_service 
ADD COLUMN second_line_manager_names VARCHAR(500) DEFAULT NULL COMMENT '二线负责人姓名列表（JSON数组，仅安全服务使用）' AFTER second_line_manager_ids;
