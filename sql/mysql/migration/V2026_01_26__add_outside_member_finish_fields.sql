-- 外出人员表添加完成状态和附件字段

-- 添加完成状态字段：0未完成 1已完成（无附件） 2已完成（有附件）
ALTER TABLE project_outside_member
ADD COLUMN finish_status TINYINT DEFAULT 0 COMMENT '完成状态：0未完成 1已完成（无附件） 2已完成（有附件）';

-- 添加完成时间字段
ALTER TABLE project_outside_member
ADD COLUMN finish_time DATETIME COMMENT '完成时间';

-- 添加附件URL字段（多个附件用逗号分隔）
ALTER TABLE project_outside_member
ADD COLUMN attachment_url VARCHAR(2000) COMMENT '附件URL（多个附件用逗号分隔）';

-- 添加完成备注字段
ALTER TABLE project_outside_member
ADD COLUMN finish_remark VARCHAR(500) COMMENT '完成备注';
