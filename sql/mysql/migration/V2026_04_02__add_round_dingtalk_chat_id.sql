ALTER TABLE project_round ADD COLUMN dingtalk_chat_id VARCHAR(128) DEFAULT NULL COMMENT '钉钉群聊ID（已废弃，群在项目级别管理）' AFTER remark;
