-- 为外出费用记录表添加服务类型和部门类型字段
-- 用于前端进行字典转换显示中文服务类型

ALTER TABLE outside_cost_record
    ADD COLUMN service_type VARCHAR(64) NULL COMMENT '服务类型（字典值）' AFTER service_item_name,
    ADD COLUMN dept_type TINYINT NULL COMMENT '部门类型：1-安全服务 2-安全运营 3-数据安全' AFTER service_type;

-- 更新现有记录的服务类型和部门类型
UPDATE outside_cost_record ocr
    LEFT JOIN project_info pi ON ocr.service_item_id = pi.id
SET ocr.service_type = pi.service_type,
    ocr.dept_type = pi.dept_type
WHERE ocr.service_type IS NULL;
