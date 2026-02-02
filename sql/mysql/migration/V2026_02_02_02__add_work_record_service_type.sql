-- 为项目工作记录表添加服务类型字段
-- 用于前端进行字典转换显示中文服务类型

ALTER TABLE project_management_record
    ADD COLUMN service_type VARCHAR(64) NULL COMMENT '服务类型（字典值）' AFTER service_item_name;

-- 更新现有记录的服务类型
UPDATE project_management_record pmr
    LEFT JOIN project_info pi ON pmr.service_item_id = pi.id
SET pmr.service_type = pi.service_type
WHERE pmr.service_type IS NULL AND pmr.service_item_id IS NOT NULL;
