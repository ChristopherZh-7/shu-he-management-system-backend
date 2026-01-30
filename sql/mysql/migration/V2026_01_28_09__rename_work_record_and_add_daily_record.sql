-- =============================================
-- 1. 重命名：工作记录 → 项目管理记录
-- =============================================

-- 1.1 重命名表
RENAME TABLE `project_work_record` TO `project_management_record`;

-- 1.2 更新字典类型
UPDATE `system_dict_type` SET 
    `type` = 'project_management_type',
    `name` = '项目管理记录类型'
WHERE `type` = 'project_work_type';

UPDATE `system_dict_data` SET 
    `dict_type` = 'project_management_type'
WHERE `dict_type` = 'project_work_type';

-- 1.3 更新菜单名称和权限
UPDATE `system_menu` SET 
    `name` = '项目管理记录',
    `path` = 'project-management-record',
    `component` = 'dashboard/project-management-record/index'
WHERE `name` = '工作记录' AND `path` = 'work-record';

UPDATE `system_menu` SET 
    `permission` = 'project:management-record:query'
WHERE `permission` = 'project:work-record:query';

UPDATE `system_menu` SET 
    `permission` = 'project:management-record:create'
WHERE `permission` = 'project:work-record:create';

UPDATE `system_menu` SET 
    `permission` = 'project:management-record:update'
WHERE `permission` = 'project:work-record:update';

UPDATE `system_menu` SET 
    `permission` = 'project:management-record:delete'
WHERE `permission` = 'project:work-record:delete';

UPDATE `system_menu` SET 
    `permission` = 'project:management-record:export'
WHERE `permission` = 'project:work-record:export';

-- =============================================
-- 2. 新增：日常管理记录表
-- =============================================

CREATE TABLE IF NOT EXISTS `daily_management_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 记录周期
    `year` INT NOT NULL COMMENT '年份',
    `week_number` INT NOT NULL COMMENT '周数（1-53）',
    `week_start_date` DATE NOT NULL COMMENT '本周开始日期（周一）',
    `week_end_date` DATE NOT NULL COMMENT '本周结束日期（周五）',
    
    -- 每日工作内容
    `monday_content` TEXT DEFAULT NULL COMMENT '周一工作内容',
    `tuesday_content` TEXT DEFAULT NULL COMMENT '周二工作内容',
    `wednesday_content` TEXT DEFAULT NULL COMMENT '周三工作内容',
    `thursday_content` TEXT DEFAULT NULL COMMENT '周四工作内容',
    `friday_content` TEXT DEFAULT NULL COMMENT '周五工作内容',
    
    -- 周总结
    `weekly_summary` TEXT DEFAULT NULL COMMENT '本周总结',
    `next_week_plan` TEXT DEFAULT NULL COMMENT '下周计划',
    
    -- 附件
    `attachments` VARCHAR(2000) DEFAULT NULL COMMENT '附件URL列表（JSON数组）',
    
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
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_creator_year_week` (`creator`, `year`, `week_number`, `deleted`) COMMENT '每人每周只能有一条记录',
    KEY `idx_year_week` (`year`, `week_number`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_creator` (`creator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常管理记录表（按周记录）';

-- =============================================
-- 3. 添加日常管理记录菜单
-- =============================================

-- 查询父菜单ID（工作台）
SET @workbench_menu_id = (SELECT `id` FROM `system_menu` WHERE `name` = '工作台' AND `path` = 'workbench' LIMIT 1);
-- 如果没有工作台，查找概览/Dashboard
SET @workbench_menu_id = IFNULL(@workbench_menu_id, (SELECT `id` FROM `system_menu` WHERE `name` IN ('概览', 'Dashboard') OR `path` = '/dashboard' LIMIT 1));
-- 如果还找不到，设为0（顶级菜单）
SET @parent_id = IFNULL(@workbench_menu_id, 0);

-- 插入日常管理记录菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('日常管理记录', '', 1, 2, @parent_id, 'daily-management-record', 'ep:calendar', 'dashboard/daily-management-record/index', 'DailyManagementRecord', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

SET @daily_menu_id = LAST_INSERT_ID();

-- 添加按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES 
('日常记录查询', 'project:daily-record:query', 2, 1, @daily_menu_id, '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0),
('日常记录创建', 'project:daily-record:create', 2, 2, @daily_menu_id, '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0),
('日常记录更新', 'project:daily-record:update', 2, 3, @daily_menu_id, '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0),
('日常记录删除', 'project:daily-record:delete', 2, 4, @daily_menu_id, '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
