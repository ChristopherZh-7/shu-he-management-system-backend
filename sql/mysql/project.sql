-- =============================================
-- 项目管理模块 - 数据库表
-- =============================================

-- ----------------------------
-- 1. 项目主表
-- ----------------------------
DROP TABLE IF EXISTS `project_info`;
CREATE TABLE `project_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `code` varchar(50) NOT NULL COMMENT '项目编号',
    `name` varchar(200) NOT NULL COMMENT '项目名称',
    `dept_type` tinyint NOT NULL COMMENT '部门类型：1安全服务 2安全运营 3数据安全',
    `service_type` varchar(50) DEFAULT NULL COMMENT '服务类型（字典值）',
    `description` text COMMENT '项目描述',
    
    -- 客户信息
    `customer_id` bigint DEFAULT NULL COMMENT 'CRM客户ID',
    `customer_name` varchar(200) DEFAULT NULL COMMENT '客户名称',
    `contract_id` bigint DEFAULT NULL COMMENT 'CRM合同ID',
    `contract_no` varchar(100) DEFAULT NULL COMMENT '合同编号',
    
    -- 时间信息
    `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    
    -- 人员信息
    `manager_id` bigint DEFAULT NULL COMMENT '项目经理ID',
    `manager_name` varchar(100) DEFAULT NULL COMMENT '项目经理姓名',
    `dept_id` bigint DEFAULT NULL COMMENT '所属部门ID',
    
    -- 状态进度
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1进行中 2已暂停 3已完成 4已取消',
    `progress` tinyint NOT NULL DEFAULT 0 COMMENT '进度百分比 0-100',
    `priority` tinyint NOT NULL DEFAULT 1 COMMENT '优先级：0低 1中 2高',
    
    -- 商务信息
    `amount` decimal(12,2) DEFAULT NULL COMMENT '项目金额',
    
    -- 扩展字段
    `tags` varchar(500) DEFAULT NULL COMMENT '标签（JSON数组）',
    `remark` text COMMENT '备注',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_dept_type` (`dept_type`),
    KEY `idx_status` (`status`),
    KEY `idx_manager_id` (`manager_id`),
    KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目信息表';

-- ----------------------------
-- 2. 项目成员表
-- ----------------------------
DROP TABLE IF EXISTS `project_member`;
CREATE TABLE `project_member` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `nickname` varchar(100) DEFAULT NULL COMMENT '用户昵称',
    `role_type` tinyint NOT NULL DEFAULT 2 COMMENT '角色类型：1项目经理 2执行人员 3审核人员',
    `join_time` datetime DEFAULT NULL COMMENT '加入时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- ----------------------------
-- 3. 字典类型 - 服务类型配置
-- ----------------------------
-- 安全服务 服务类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('安全服务-服务类型', 'project_service_type_security', 0, '安全服务部门的服务类型', 'admin', NOW(), 'admin', NOW(), b'0');

-- 安全运营 服务类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('安全运营-服务类型', 'project_service_type_operation', 0, '安全运营部门的服务类型', 'admin', NOW(), 'admin', NOW(), b'0');

-- 数据安全 服务类型
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('数据安全-服务类型', 'project_service_type_data', 0, '数据安全部门的服务类型', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 4. 字典数据 - 安全服务
-- ----------------------------
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '渗透测试', 'penetration_test', 'project_service_type_security', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '等保测评', 'level_protection', 'project_service_type_security', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '代码审计', 'code_audit', 'project_service_type_security', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '风险评估', 'risk_assessment', 'project_service_type_security', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '安全咨询', 'security_consulting', 'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, '应急响应', 'emergency_response', 'project_service_type_security', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, '安全培训', 'security_training', 'project_service_type_security', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(8, '漏洞扫描', 'vulnerability_scan', 'project_service_type_security', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 5. 字典数据 - 安全运营
-- ----------------------------
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '威胁监控', 'threat_monitoring', 'project_service_type_operation', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '安全巡检', 'security_inspection', 'project_service_type_operation', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '攻防演练', 'attack_defense_drill', 'project_service_type_operation', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '值守服务', 'duty_service', 'project_service_type_operation', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '漏洞管理', 'vulnerability_management', 'project_service_type_operation', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, '安全加固', 'security_hardening', 'project_service_type_operation', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, '日志分析', 'log_analysis', 'project_service_type_operation', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 6. 字典数据 - 数据安全
-- ----------------------------
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '数据分类分级', 'data_classification', 'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '数据脱敏', 'data_masking', 'project_service_type_data', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '数据加密', 'data_encryption', 'project_service_type_data', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '合规审计', 'compliance_audit', 'project_service_type_data', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '数据备份', 'data_backup', 'project_service_type_data', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(6, '数据恢复', 'data_recovery', 'project_service_type_data', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(7, '隐私保护', 'privacy_protection', 'project_service_type_data', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 7. 项目状态字典
-- ----------------------------
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('项目状态', 'project_status', 0, '项目状态', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(0, '草稿', '0', 'project_status', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(1, '进行中', '1', 'project_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '已暂停', '2', 'project_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '已完成', '3', 'project_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, '已取消', '4', 'project_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 8. 项目优先级字典
-- ----------------------------
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('项目优先级', 'project_priority', 0, '项目优先级', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(0, '低', '0', 'project_priority', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(1, '中', '1', 'project_priority', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '高', '2', 'project_priority', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- ----------------------------
-- 9. 成员角色字典
-- ----------------------------
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('项目成员角色', 'project_member_role', 0, '项目成员角色类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '项目经理', '1', 'project_member_role', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '执行人员', '2', 'project_member_role', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '审核人员', '3', 'project_member_role', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 10. 菜单权限数据
-- =============================================

-- 项目管理一级菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('项目管理', '', 1, 70, 0, '/project', 'ep:folder-opened', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @project_menu_id = LAST_INSERT_ID();

-- 安全服务
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('安全服务', '', 2, 1, @project_menu_id, 'security-service', 'ep:lock', 'project/security-service/index', 'ProjectSecurityService', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @security_service_menu_id = LAST_INSERT_ID();

-- 安全服务按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('项目查询', 'project:info:query', 3, 1, @security_service_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目创建', 'project:info:create', 3, 2, @security_service_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目更新', 'project:info:update', 3, 3, @security_service_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目删除', 'project:info:delete', 3, 4, @security_service_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 安全运营
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('安全运营', '', 2, 2, @project_menu_id, 'security-operation', 'ep:monitor', 'project/security-operation/index', 'ProjectSecurityOperation', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @security_operation_menu_id = LAST_INSERT_ID();

-- 安全运营按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('项目查询', 'project:info:query', 3, 1, @security_operation_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目创建', 'project:info:create', 3, 2, @security_operation_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目更新', 'project:info:update', 3, 3, @security_operation_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目删除', 'project:info:delete', 3, 4, @security_operation_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 数据安全
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('数据安全', '', 2, 3, @project_menu_id, 'data-security', 'ep:data-analysis', 'project/data-security/index', 'ProjectDataSecurity', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @data_security_menu_id = LAST_INSERT_ID();

-- 数据安全按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('项目查询', 'project:info:query', 3, 1, @data_security_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目创建', 'project:info:create', 3, 2, @data_security_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目更新', 'project:info:update', 3, 3, @data_security_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('项目删除', 'project:info:delete', 3, 4, @data_security_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 11. 项目轮次表
-- =============================================
DROP TABLE IF EXISTS `project_round`;
CREATE TABLE `project_round` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮次ID',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `round_no` int NOT NULL COMMENT '轮次序号（第几次）',
    `name` varchar(200) DEFAULT NULL COMMENT '轮次名称（如：第1次渗透测试）',
    
    -- 时间信息
    `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    
    -- 执行信息（支持多人）
    `executor_ids` varchar(500) DEFAULT NULL COMMENT '执行人ID列表（JSON数组）',
    `executor_names` varchar(500) DEFAULT NULL COMMENT '执行人姓名列表（逗号分隔）',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待执行 1执行中 2已完成 3已取消',
    `progress` tinyint NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    
    -- 结果信息
    `result` text COMMENT '执行结果/报告摘要',
    `attachments` varchar(1000) DEFAULT NULL COMMENT '附件（JSON数组）',
    `remark` text COMMENT '备注',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目轮次表';

-- 轮次状态字典
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('项目轮次状态', 'project_round_status', 0, '项目轮次执行状态', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(0, '待执行', '0', 'project_round_status', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(1, '执行中', '1', 'project_round_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '已完成', '2', 'project_round_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '已取消', '3', 'project_round_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- 项目详情页（隐藏菜单，用于路由跳转）
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('项目详情', '', 2, 99, @project_menu_id, 'detail/:id', '', 'project/detail/index', 'ProjectDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 12. 轮次测试目标表
-- =============================================
DROP TABLE IF EXISTS `project_round_target`;
CREATE TABLE `project_round_target` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '目标ID',
    `round_id` bigint NOT NULL COMMENT '轮次ID',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `name` varchar(200) NOT NULL COMMENT '目标名称（如：官网、APP、小程序）',
    `url` varchar(500) DEFAULT NULL COMMENT '目标地址/URL',
    `type` varchar(50) DEFAULT NULL COMMENT '目标类型：web/app/miniprogram/api/other',
    `description` text COMMENT '目标描述',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_round_id` (`round_id`),
    KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮次测试目标表';

-- =============================================
-- 13. 漏洞表
-- =============================================
DROP TABLE IF EXISTS `project_round_vulnerability`;
CREATE TABLE `project_round_vulnerability` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '漏洞ID',
    `round_id` bigint NOT NULL COMMENT '轮次ID',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `target_id` bigint DEFAULT NULL COMMENT '目标ID（关联测试目标）',
    
    -- 漏洞基本信息
    `location` varchar(500) NOT NULL COMMENT '漏洞位置（标题/名称）',
    `url` varchar(1000) DEFAULT NULL COMMENT '漏洞URL',
    `severity` varchar(20) NOT NULL COMMENT '危害程度：high/medium/low',
    `type` varchar(100) NOT NULL COMMENT '漏洞类型（如：SQL注入、XSS等）',
    `process` mediumtext COMMENT '漏洞过程描述（富文本HTML）',
    
    -- 漏洞类型详情
    `type_description` text COMMENT '漏洞类型说明',
    `type_advice` text COMMENT '修复建议',
    
    -- 复测信息
    `retest_status` varchar(20) DEFAULT NULL COMMENT '复测状态：fixed/unfixed/partially-fixed',
    `retest_report` mediumtext COMMENT '复测报告内容（富文本HTML）',
    `retest_date` date DEFAULT NULL COMMENT '复测日期',
    `retest_time` datetime DEFAULT NULL COMMENT '复测时间',
    
    -- 通用字段
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    KEY `idx_round_id` (`round_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮次漏洞表';

-- =============================================
-- 14. 漏洞危害程度字典
-- =============================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('漏洞危害程度', 'vulnerability_severity', 0, '漏洞危害等级', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '高危', 'high', 'vulnerability_severity', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '中危', 'medium', 'vulnerability_severity', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '低危', 'low', 'vulnerability_severity', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 15. 漏洞复测状态字典
-- =============================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('漏洞复测状态', 'vulnerability_retest_status', 0, '漏洞复测结果状态', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '已修复', 'fixed', 'vulnerability_retest_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '未修复', 'unfixed', 'vulnerability_retest_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '部分修复', 'partially-fixed', 'vulnerability_retest_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 16. 测试目标类型字典
-- =============================================
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('测试目标类型', 'round_target_type', 0, '测试目标系统类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'Web应用', 'web', 'round_target_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, 'APP', 'app', 'round_target_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '小程序', 'miniprogram', 'round_target_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, 'API接口', 'api', 'round_target_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '其他', 'other', 'round_target_type', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 17. 轮次详情页（隐藏菜单）
-- =============================================
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('轮次详情', '', 2, 100, @project_menu_id, 'round/:roundId', '', 'project/round/index', 'ProjectRoundDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');
