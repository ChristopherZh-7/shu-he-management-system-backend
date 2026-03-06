-- 商机审批固定群（配置后不再每个商机建群，所有审批发到此群，需手动建群并加应用机器人一次）
ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `business_audit_chat_id` VARCHAR(128) NULL COMMENT '商机审批固定群chatId（配置后所有商机审批发到此群）' AFTER `outside_type`;
