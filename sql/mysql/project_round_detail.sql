-- =============================================
-- 项目轮次详情相关表和字典数据
-- 执行时间: 2026-01-15
-- =============================================

-- =============================================
-- 1. 轮次测试目标表
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
-- 2. 漏洞表
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
-- 3. 漏洞危害程度字典（先删除已存在的）
-- =============================================
DELETE FROM `system_dict_data` WHERE `dict_type` = 'vulnerability_severity';
DELETE FROM `system_dict_type` WHERE `type` = 'vulnerability_severity';

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('漏洞危害程度', 'vulnerability_severity', 0, '漏洞危害等级', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '高危', 'high', 'vulnerability_severity', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '中危', 'medium', 'vulnerability_severity', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '低危', 'low', 'vulnerability_severity', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 4. 漏洞复测状态字典
-- =============================================
DELETE FROM `system_dict_data` WHERE `dict_type` = 'vulnerability_retest_status';
DELETE FROM `system_dict_type` WHERE `type` = 'vulnerability_retest_status';

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('漏洞复测状态', 'vulnerability_retest_status', 0, '漏洞复测结果状态', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, '已修复', 'fixed', 'vulnerability_retest_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, '未修复', 'unfixed', 'vulnerability_retest_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '部分修复', 'partially-fixed', 'vulnerability_retest_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 5. 测试目标类型字典
-- =============================================
DELETE FROM `system_dict_data` WHERE `dict_type` = 'round_target_type';
DELETE FROM `system_dict_type` WHERE `type` = 'round_target_type';

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
VALUES ('测试目标类型', 'round_target_type', 0, '测试目标系统类型', 'admin', NOW(), 'admin', NOW(), b'0');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(1, 'Web应用', 'web', 'round_target_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(2, 'APP', 'app', 'round_target_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(3, '小程序', 'miniprogram', 'round_target_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(4, 'API接口', 'api', 'round_target_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), b'0'),
(5, '其他', 'other', 'round_target_type', 0, 'default', '', '', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 6. 清理重复菜单
-- =============================================
-- 删除重复的项目详情菜单（保留一个）
DELETE FROM `system_menu` WHERE `component_name` = 'ProjectDetail' AND `id` NOT IN (
    SELECT id FROM (SELECT MIN(id) as id FROM `system_menu` WHERE `component_name` = 'ProjectDetail') tmp
);

-- 删除已存在的轮次详情菜单
DELETE FROM `system_menu` WHERE `component_name` = 'ProjectRoundDetail';

-- =============================================
-- 7. 轮次详情页（隐藏菜单）
-- =============================================
-- 获取项目管理菜单ID并插入轮次详情菜单
SET @project_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '项目管理' AND `type` = 1 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('轮次详情', '', 2, 100, @project_menu_id, 'round/:roundId', '', 'project/round/index', 'ProjectRoundDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');

-- 查看菜单结果
SELECT * FROM `system_menu` WHERE `component_name` IN ('ProjectDetail', 'ProjectRoundDetail');
 