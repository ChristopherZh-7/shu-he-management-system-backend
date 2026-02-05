-- =============================================
-- 钉钉群机器人配置表
-- 支持Webhook机器人，可在群里发送消息并@指定人员
-- =============================================

-- 创建钉钉群机器人配置表
CREATE TABLE IF NOT EXISTS `system_dingtalk_robot` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '机器人编号',
    `name` varchar(64) NOT NULL COMMENT '机器人名称',
    `webhook_url` varchar(512) NOT NULL COMMENT 'Webhook地址',
    `secret` varchar(128) DEFAULT NULL COMMENT '加签密钥（安全设置为加签时必填）',
    `security_type` tinyint NOT NULL DEFAULT 1 COMMENT '安全类型（1-关键词 2-加签 3-IP白名单）',
    `keywords` varchar(512) DEFAULT NULL COMMENT '关键词列表（JSON数组，安全类型为关键词时使用）',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0-正常 1-停用）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉群机器人配置表';

-- 创建群机器人消息发送记录表（用于追踪消息发送历史）
CREATE TABLE IF NOT EXISTS `system_dingtalk_robot_message` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息编号',
    `robot_id` bigint NOT NULL COMMENT '机器人编号',
    `msg_type` varchar(32) NOT NULL COMMENT '消息类型（text-文本、markdown-Markdown、actionCard-卡片、link-链接）',
    `content` text NOT NULL COMMENT '消息内容（JSON格式）',
    `at_mobiles` varchar(1024) DEFAULT NULL COMMENT '@的手机号列表（JSON数组）',
    `at_user_ids` varchar(1024) DEFAULT NULL COMMENT '@的用户ID列表（JSON数组）',
    `is_at_all` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否@所有人',
    `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态（0-成功 1-失败）',
    `error_msg` varchar(1024) DEFAULT NULL COMMENT '错误信息',
    `response_data` text DEFAULT NULL COMMENT '钉钉返回数据',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_robot_id` (`robot_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉群机器人消息记录表';
