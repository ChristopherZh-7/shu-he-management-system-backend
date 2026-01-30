-- =============================================
-- 修复工作台菜单结构
-- 解决日常管理记录页面无法访问的问题
-- 
-- 问题原因：
-- 1. 数据库中 component_name 与前端路由 name 不一致
-- 2. 工作台的 path 有前导斜杠 /workbench（应该是 workbench）
-- =============================================

-- 1. 修复工作台 path（去掉前导斜杠）
UPDATE `system_menu`
SET 
    `path` = 'workbench',
    `updater` = '1',
    `update_time` = NOW()
WHERE `path` = '/workbench' AND `deleted` = 0;

-- 2. 修复日常管理记录的 component_name（与前端路由 name 保持一致）
UPDATE `system_menu`
SET 
    `component_name` = 'WorkbenchDailyManagementRecord',
    `updater` = '1',
    `update_time` = NOW()
WHERE `name` = '日常管理记录' AND `deleted` = 0;

-- 3. 修复项目管理记录的 component_name（与前端路由 name 保持一致）
UPDATE `system_menu`
SET 
    `component_name` = 'WorkbenchProjectManagementRecord',
    `updater` = '1',
    `update_time` = NOW()
WHERE `name` = '项目管理记录' AND `deleted` = 0;

-- 也兼容可能存在的"工作记录"菜单名称
UPDATE `system_menu`
SET 
    `component_name` = 'WorkbenchProjectManagementRecord',
    `updater` = '1',
    `update_time` = NOW()
WHERE `name` = '工作记录' AND `component` = 'dashboard/work-record/index' AND `deleted` = 0;

-- 4. 输出修复结果
SELECT 'Workbench menu structure fixed!' AS result;

-- 验证修复结果
SELECT id, name, parent_id, path, component, component_name, visible, status
FROM system_menu 
WHERE name IN ('工作台', '日常管理记录', '项目管理记录', '工作记录')
   OR path IN ('workbench', '/workbench', 'daily-management-record', 'project-management-record')
ORDER BY parent_id, id;
