-- 钉钉配置表新增外出确认相关字段
ALTER TABLE `system_dingtalk_config` 
ADD COLUMN `callback_base_url` VARCHAR(255) DEFAULT NULL COMMENT '回调基础URL（公网可访问域名）',
ADD COLUMN `outside_process_code` VARCHAR(64) DEFAULT NULL COMMENT '钉钉OA外出申请流程编码',
ADD COLUMN `outside_type` VARCHAR(50) DEFAULT '因公外出' COMMENT '默认外出类型';
