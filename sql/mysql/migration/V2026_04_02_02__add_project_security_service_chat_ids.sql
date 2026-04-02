ALTER TABLE project ADD COLUMN dingtalk_onsite_chat_id VARCHAR(128) DEFAULT NULL COMMENT '安全服务-驻场钉钉群ID' AFTER dingtalk_chat_id;
ALTER TABLE project ADD COLUMN dingtalk_second_line_chat_id VARCHAR(128) DEFAULT NULL COMMENT '安全服务-二线钉钉群ID' AFTER dingtalk_onsite_chat_id;
