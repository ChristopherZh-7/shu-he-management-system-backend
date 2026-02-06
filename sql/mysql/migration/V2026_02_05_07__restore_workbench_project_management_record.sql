-- =============================================
-- 修复：恢复工作台完整菜单结构
-- 原因：V2026_02_05_06 脚本误删了工作台的子菜单
-- 
-- 重要：component 必须设置正确的路径！
-- 前端 pageMap 会扫描 views/**/*.vue，所以要匹配这个路径格式
-- 
-- 工作台正确结构：
-- └── 工作台 (workbench)  -> component: ''
--     ├── 周工作日历 (weekly-work-calendar)     -> dashboard/weekly-work-calendar/index
--     ├── 项目管理记录 (project-management-record) -> dashboard/work-record/index
--     └── 日常管理记录 (daily-management-record)   -> dashboard/daily-management-record/index
-- =============================================

-- 1. 获取工作台菜单ID
SET @workbench_menu_id = (
    SELECT `id` FROM `system_menu` 
    WHERE (`name` = '工作台' OR `path` = 'workbench') 
      AND `deleted` = 0 
    LIMIT 1
);

SELECT CONCAT('工作台菜单ID: ', IFNULL(@workbench_menu_id, '未找到')) AS info;

-- 2. 清理错误的按钮权限菜单
DELETE FROM `system_menu` 
WHERE `name` LIKE '%项目管理记录%' 
  AND `type` = 2 
  AND `permission` != '';

-- =============================================
-- 3. 恢复/创建：周工作日历
-- =============================================
-- 尝试恢复软删除的记录
UPDATE `system_menu` 
SET `deleted` = 0, 
    `update_time` = NOW(), 
    `updater` = '1',
    `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/weekly-work-calendar/index',
    `component_name` = 'WorkbenchWeeklyWorkCalendar',
    `type` = 2
WHERE `path` = 'weekly-work-calendar' AND `deleted` = 1;

-- 如果不存在则插入
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 
    '周工作日历', '', 2, 0, @workbench_menu_id, 'weekly-work-calendar', 
    'ep:calendar', 'dashboard/weekly-work-calendar/index', 'WorkbenchWeeklyWorkCalendar',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0
) AND @workbench_menu_id IS NOT NULL;

-- 确保已存在的周工作日历配置正确
UPDATE `system_menu` 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/weekly-work-calendar/index',
    `component_name` = 'WorkbenchWeeklyWorkCalendar',
    `type` = 2,
    `update_time` = NOW()
WHERE `path` = 'weekly-work-calendar' 
  AND `deleted` = 0
  AND @workbench_menu_id IS NOT NULL;

-- =============================================
-- 4. 恢复/创建：项目管理记录
-- =============================================
-- 尝试恢复软删除的记录
UPDATE `system_menu` 
SET `deleted` = 0, 
    `update_time` = NOW(), 
    `updater` = '1',
    `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/work-record/index',
    `component_name` = 'WorkbenchProjectManagementRecord',
    `type` = 2
WHERE `path` = 'project-management-record' AND `deleted` = 1;

-- 如果不存在则插入
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 
    '项目管理记录', '', 2, 1, @workbench_menu_id, 'project-management-record', 
    'ep:document', 'dashboard/work-record/index', 'WorkbenchProjectManagementRecord',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `path` = 'project-management-record' AND `deleted` = 0
) AND @workbench_menu_id IS NOT NULL;

-- 确保已存在的项目管理记录配置正确
UPDATE `system_menu` 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/work-record/index',
    `component_name` = 'WorkbenchProjectManagementRecord',
    `type` = 2,
    `update_time` = NOW()
WHERE `path` = 'project-management-record' 
  AND `deleted` = 0
  AND @workbench_menu_id IS NOT NULL;

-- =============================================
-- 5. 确保：日常管理记录 配置正确
-- =============================================
UPDATE `system_menu` 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/daily-management-record/index',
    `component_name` = 'WorkbenchDailyManagementRecord',
    `type` = 2,
    `update_time` = NOW()
WHERE `path` = 'daily-management-record' 
  AND `deleted` = 0
  AND @workbench_menu_id IS NOT NULL;

-- =============================================
-- 6. 确保工作台本身配置正确（目录类型，无 component）
-- =============================================
UPDATE `system_menu`
SET `component` = '',
    `component_name` = 'Workbench',
    `type` = 1,
    `update_time` = NOW()
WHERE `id` = @workbench_menu_id AND `deleted` = 0;

-- =============================================
-- 7. 为超级管理员角色分配菜单权限
-- =============================================
SET @weekly_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0 LIMIT 1);
SET @pm_record_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'project-management-record' AND `deleted` = 0 LIMIT 1);
SET @daily_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'daily-management-record' AND `deleted` = 0 LIMIT 1);

SET @admin_role_id = 1;

-- 分配工作台及其子菜单
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, @workbench_menu_id, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @workbench_menu_id IS NOT NULL;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, @weekly_menu_id, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @weekly_menu_id IS NOT NULL;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, @pm_record_menu_id, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pm_record_menu_id IS NOT NULL;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, @daily_menu_id, '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @daily_menu_id IS NOT NULL;

-- =============================================
-- 8. 验证修复结果
-- =============================================
SELECT '===== 工作台菜单结构（修复后） =====' AS title;

SELECT 
    m.id,
    m.name,
    m.path,
    m.component,
    m.component_name,
    m.parent_id,
    m.sort,
    CASE m.type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END AS menu_type
FROM `system_menu` m
WHERE (m.id = @workbench_menu_id 
       OR m.parent_id = @workbench_menu_id)
  AND m.deleted = 0
ORDER BY 
    CASE WHEN m.id = @workbench_menu_id THEN 0 ELSE 1 END,
    m.sort, m.id;

SELECT '修复完成！请重新登录或 Ctrl+Shift+R 强制刷新。' AS result;
