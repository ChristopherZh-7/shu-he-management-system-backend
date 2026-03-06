-- 商机审批场景群模板ID（配置后使用场景群建群，机器人自动进群）
ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `business_audit_template_id` VARCHAR(128) NULL COMMENT '商机审批场景群模板ID（配置后建群自动带机器人）' AFTER `business_audit_chat_id`;
