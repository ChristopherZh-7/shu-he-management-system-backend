-- 为服务项表增加 visible 字段
-- 用于控制服务项在项目详情中的可见性
-- 普通服务项：visible = 1（默认可见）
-- 外出服务项：创建时 visible = 0（隐藏），审批通过后变为 visible = 1

ALTER TABLE project_info ADD COLUMN visible TINYINT(1) DEFAULT 1 COMMENT '是否可见：0隐藏 1可见';

-- 更新索引（可选，如果查询频繁可以加上）
-- CREATE INDEX idx_project_info_visible ON project_info(visible);
