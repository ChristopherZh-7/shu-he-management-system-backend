-- 仅添加 approve_base_url（前两列已存在时使用）
ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `approve_base_url` VARCHAR(255) NULL COMMENT '审批链接baseUrl' AFTER `callback_base_url`;
