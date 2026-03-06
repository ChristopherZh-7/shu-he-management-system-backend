-- 修复：钉钉配置缺少商机审批相关字段
-- 本地开发若未跑 deploy.ps1 -Database，可手动执行本脚本
-- 若某列已存在会报 Duplicate column，可注释掉对应 ALTER

ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `business_audit_chat_id` VARCHAR(128) NULL COMMENT '商机审批固定群chatId' AFTER `outside_type`;
ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `business_audit_template_id` VARCHAR(128) NULL COMMENT '商机审批场景群模板ID' AFTER `business_audit_chat_id`;
