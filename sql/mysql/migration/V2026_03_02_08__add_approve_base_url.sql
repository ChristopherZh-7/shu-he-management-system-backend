-- 审批链接使用的前端/网关 baseUrl（可访问的入口，如 http://localhost:5666）
-- 不填则用 callback_base_url。用于生成「通过审批」「驳回」「修改金额」等链接
ALTER TABLE `system_dingtalk_config`
    ADD COLUMN `approve_base_url` VARCHAR(255) NULL COMMENT '审批链接baseUrl（前端/网关入口）' AFTER `callback_base_url`;
