-- 项目工作记录表
-- 用于记录每个项目/服务项的日常工作内容

CREATE TABLE IF NOT EXISTS `project_work_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 项目关联
    `project_id` BIGINT NOT NULL COMMENT '项目ID',
    `project_type` TINYINT NOT NULL COMMENT '项目类型: 1-安全服务 2-安全运营 3-数据安全',
    `project_name` VARCHAR(255) DEFAULT NULL COMMENT '项目名称(冗余便于查询)',
    
    -- 服务项关联（可选）
    `service_item_id` BIGINT DEFAULT NULL COMMENT '服务项ID(可选)',
    `service_item_name` VARCHAR(255) DEFAULT NULL COMMENT '服务项名称(冗余)',
    
    -- 记录内容
    `record_date` DATE NOT NULL COMMENT '记录日期',
    `work_type` VARCHAR(50) DEFAULT NULL COMMENT '工作类型: patrol-巡检, meeting-会议, report-报告, incident-事件处理, training-培训, maintenance-维护, other-其他',
    `work_content` TEXT NOT NULL COMMENT '工作内容',
    `attachments` VARCHAR(2000) DEFAULT NULL COMMENT '附件URL(JSON数组)',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    
    -- 记录人信息
    `creator_name` VARCHAR(100) DEFAULT NULL COMMENT '记录人姓名(冗余)',
    `dept_id` BIGINT DEFAULT NULL COMMENT '记录人部门ID',
    `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称(冗余)',
    
    -- 审计字段（与 BaseDO 保持一致）
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    
    PRIMARY KEY (`id`),
    INDEX `idx_project_id` (`project_id`),
    INDEX `idx_service_item_id` (`service_item_id`),
    INDEX `idx_record_date` (`record_date`),
    INDEX `idx_creator` (`creator`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_project_type` (`project_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目工作记录表';

-- 添加工作类型字典
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('项目工作类型', 'project_work_type', 0, '项目工作记录的工作类型', 1, NOW(), 1, NOW(), b'0');

-- 获取刚插入的字典类型ID（使用变量）
SET @dict_type_id = LAST_INSERT_ID();

-- 添加工作类型字典数据
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES 
(1, '日常巡检', 'patrol', 'project_work_type', 0, 'primary', '', '日常巡检工作', 1, NOW(), 1, NOW(), b'0'),
(2, '会议沟通', 'meeting', 'project_work_type', 0, 'info', '', '会议沟通记录', 1, NOW(), 1, NOW(), b'0'),
(3, '报告输出', 'report', 'project_work_type', 0, 'success', '', '报告输出工作', 1, NOW(), 1, NOW(), b'0'),
(4, '事件处理', 'incident', 'project_work_type', 0, 'warning', '', '事件处理记录', 1, NOW(), 1, NOW(), b'0'),
(5, '培训演练', 'training', 'project_work_type', 0, 'default', '', '培训演练活动', 1, NOW(), 1, NOW(), b'0'),
(6, '维护保养', 'maintenance', 'project_work_type', 0, 'default', '', '维护保养工作', 1, NOW(), 1, NOW(), b'0'),
(7, '其他', 'other', 'project_work_type', 0, 'default', '', '其他工作', 1, NOW(), 1, NOW(), b'0');

-- 添加菜单：工作记录（放在 概览/Dashboard 下）
-- 先查询 概览/Dashboard 菜单的ID（兼容不同名称）
SET @dashboard_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` IN ('概览', 'Dashboard') OR `path` = '/dashboard' LIMIT 1);

-- 如果找不到，则创建为一级菜单
SET @parent_id = IFNULL(@dashboard_menu_id, 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('工作记录', '', 2, 3, @parent_id, 'work-record', 'ep:document', 'dashboard/work-record/index', 'WorkRecord', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0');

-- 获取刚插入的菜单ID
SET @work_record_menu_id = LAST_INSERT_ID();

-- 添加按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES 
('工作记录查询', 'project:work-record:query', 3, 1, @work_record_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0'),
('工作记录新增', 'project:work-record:create', 3, 2, @work_record_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0'),
('工作记录修改', 'project:work-record:update', 3, 3, @work_record_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0'),
('工作记录删除', 'project:work-record:delete', 3, 4, @work_record_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0'),
('工作记录导出', 'project:work-record:export', 3, 5, @work_record_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 1, NOW(), 1, NOW(), b'0');
