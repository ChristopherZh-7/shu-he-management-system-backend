-- =============================================
-- 钉钉群机器人通知场景配置
-- 支持配置业务事件触发自动发送钉钉群消息
-- =============================================

-- 通知场景配置表
CREATE TABLE IF NOT EXISTS `system_dingtalk_notification_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置编号',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `robot_id` BIGINT NOT NULL COMMENT '关联的机器人ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型（如：contract_create, contract_update等）',
    `event_module` VARCHAR(50) NOT NULL COMMENT '事件所属模块（如：crm, project, bpm等）',
    `msg_type` VARCHAR(20) NOT NULL DEFAULT 'markdown' COMMENT '消息类型（text/markdown/link/actionCard）',
    `title_template` VARCHAR(200) DEFAULT NULL COMMENT '标题模板（支持变量如${contractName}）',
    `content_template` TEXT NOT NULL COMMENT '内容模板（支持变量替换）',
    `at_type` TINYINT NOT NULL DEFAULT 0 COMMENT '@类型：0-不@任何人，1-@负责人，2-@创建人，3-@指定人员，4-@所有人',
    `at_mobiles` VARCHAR(500) DEFAULT NULL COMMENT '@的手机号列表（JSON数组，at_type=3时使用）',
    `at_user_ids` VARCHAR(500) DEFAULT NULL COMMENT '@的用户ID列表（JSON数组，at_type=3时使用）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-启用 1-停用）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_robot_id` (`robot_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_event_module` (`event_module`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉通知场景配置表';

-- 通知发送日志表（记录每次自动发送的结果）
CREATE TABLE IF NOT EXISTS `system_dingtalk_notification_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志编号',
    `config_id` BIGINT NOT NULL COMMENT '配置ID',
    `robot_id` BIGINT NOT NULL COMMENT '机器人ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型',
    `event_module` VARCHAR(50) NOT NULL COMMENT '事件模块',
    `business_id` BIGINT DEFAULT NULL COMMENT '业务数据ID（如合同ID）',
    `business_no` VARCHAR(100) DEFAULT NULL COMMENT '业务编号（如合同编号）',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '发送的标题',
    `content` TEXT COMMENT '发送的内容',
    `at_mobiles` VARCHAR(500) DEFAULT NULL COMMENT '@的手机号',
    `send_status` TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态（0-成功 1-失败）',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_business_id` (`business_id`),
    KEY `idx_send_status` (`send_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉通知发送日志表';

-- 插入一些默认的示例配置
INSERT INTO `system_dingtalk_notification_config` 
(`name`, `robot_id`, `event_type`, `event_module`, `msg_type`, `title_template`, `content_template`, `at_type`, `status`, `remark`, `creator`, `updater`) 
VALUES 
('合同创建通知', 1, 'contract_create', 'crm', 'markdown', '新合同创建通知', 
'### 新合同创建\n- **合同名称**: ${name}\n- **合同金额**: ${totalPrice} 元\n- **客户**: ${customerName}\n- **负责人**: ${ownerUserName}\n- **创建时间**: ${createTime}', 
1, 1, '合同创建时自动通知负责人（默认停用，启用后生效）', 'system', 'system'),

('合同审核通过通知', 1, 'contract_audit_pass', 'crm', 'markdown', '合同审核通过',
'### 合同审核通过\n- **合同名称**: ${name}\n- **合同金额**: ${totalPrice} 元\n- **客户**: ${customerName}\n- **审核人**: ${auditorName}\n- **审核时间**: ${auditTime}',
1, 1, '合同审核通过时通知负责人（默认停用）', 'system', 'system'),

('回款提醒通知', 1, 'receivable_plan_remind', 'crm', 'markdown', '回款计划提醒',
'### 回款计划提醒\n- **客户**: ${customerName}\n- **合同**: ${contractName}\n- **计划回款日期**: ${returnTime}\n- **计划回款金额**: ${price} 元\n- **期数**: 第${period}期',
1, 1, '回款计划到期提醒（默认停用）', 'system', 'system'),

('项目创建通知', 1, 'project_create', 'project', 'markdown', '新项目创建通知',
'### 新项目创建\n- **项目名称**: ${name}\n- **客户**: ${customerName}\n- **项目经理**: ${managerName}\n- **创建时间**: ${createTime}',
1, 1, '项目创建时通知相关人员（默认停用）', 'system', 'system');
