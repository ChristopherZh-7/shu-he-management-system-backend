-- 项目表新增钉钉群会话ID和退场相关字段
-- dingtalk_chat_id: 关联商机的钉钉群ID，用于退场时发送通知
-- status=3 表示已退场（原有：0草稿 1进行中 2已完成）
ALTER TABLE project
    ADD COLUMN dingtalk_chat_id VARCHAR(128) DEFAULT NULL COMMENT '关联商机的钉钉群会话ID，退场通知使用',
    ADD COLUMN exit_remark VARCHAR(512) DEFAULT NULL COMMENT '退场备注';
