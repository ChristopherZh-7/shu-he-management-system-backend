-- 项目增加类型字段：1=驻场类 2=管理类
ALTER TABLE project ADD COLUMN project_type TINYINT NOT NULL DEFAULT 1 COMMENT '项目类型：1-驻场类 2-管理类' AFTER dept_type;
