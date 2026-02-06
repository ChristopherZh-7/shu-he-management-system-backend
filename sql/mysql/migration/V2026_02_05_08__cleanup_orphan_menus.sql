-- =============================================
-- 全面清理菜单结构
-- 
-- 问题：工作台下面出现了很多不应该属于它的菜单和按钮
-- 这些菜单/按钮的 parent_id 被错误地设置了
-- =============================================

-- =============================================
-- 1. 首先查看当前工作台的菜单结构（调试用）
-- =============================================
SET @workbench_menu_id = (
    SELECT `id` FROM `system_menu` 
    WHERE `path` = 'workbench' 
      AND `deleted` = 0 
    LIMIT 1
);

SELECT CONCAT('工作台菜单ID: ', IFNULL(@workbench_menu_id, '未找到')) AS info;

SELECT '===== 清理前：工作台下的所有菜单 =====' AS title;
SELECT id, name, path, permission, type, sort, parent_id
FROM system_menu 
WHERE parent_id = @workbench_menu_id AND deleted = 0
ORDER BY sort, id;

-- =============================================
-- 2. 工作台只应该保留这 3 个子菜单（按 path 识别）
-- - weekly-work-calendar (周工作日历)
-- - project-management-record (项目管理记录)
-- - daily-management-record (日常管理记录)
-- =============================================

-- 2.1 删除工作台下所有不应该存在的菜单
-- 保留条件：path 是上述三个之一
-- 注意：按钮类型的菜单 path 可能为空，所以也要根据 name 来判断
DELETE FROM system_menu 
WHERE parent_id = @workbench_menu_id 
  AND deleted = 0
  AND (
    -- path 不是三个允许的值
    (path NOT IN ('weekly-work-calendar', 'project-management-record', 'daily-management-record') AND path != '')
    -- 或者 path 为空（说明是按钮，不应该在工作台下）
    OR (path = '' OR path IS NULL)
  )
  -- 额外排除保留的三个菜单（以防万一）
  AND name NOT IN ('周工作日历', '项目管理记录', '日常管理记录');

-- =============================================
-- 3. 清理所有孤儿菜单（parent_id 指向不存在的菜单）
-- =============================================

-- 3.1 先获取所有有效的菜单 ID
CREATE TEMPORARY TABLE IF NOT EXISTS temp_valid_menu_ids AS
SELECT id FROM system_menu WHERE deleted = 0;

-- 3.2 删除那些 parent_id 指向不存在菜单的记录（但 parent_id != 0）
DELETE FROM system_menu 
WHERE deleted = 0
  AND parent_id != 0
  AND parent_id NOT IN (SELECT id FROM temp_valid_menu_ids);

DROP TEMPORARY TABLE IF EXISTS temp_valid_menu_ids;

-- =============================================
-- 4. 清理角色菜单关联中的无效记录
-- =============================================
DELETE FROM system_role_menu 
WHERE menu_id NOT IN (
    SELECT id FROM system_menu WHERE deleted = 0
);

-- =============================================
-- 5. 确保工作台三个子菜单配置正确
-- =============================================

-- 5.1 周工作日历
UPDATE system_menu 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/weekly-work-calendar/index',
    `component_name` = 'WorkbenchWeeklyWorkCalendar',
    `type` = 2,
    `sort` = 0,
    `update_time` = NOW()
WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0;

-- 5.2 项目管理记录
UPDATE system_menu 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/work-record/index',
    `component_name` = 'WorkbenchProjectManagementRecord',
    `type` = 2,
    `sort` = 1,
    `update_time` = NOW()
WHERE `path` = 'project-management-record' AND `deleted` = 0;

-- 5.3 日常管理记录
UPDATE system_menu 
SET `parent_id` = @workbench_menu_id,
    `component` = 'dashboard/daily-management-record/index',
    `component_name` = 'WorkbenchDailyManagementRecord',
    `type` = 2,
    `sort` = 2,
    `update_time` = NOW()
WHERE `path` = 'daily-management-record' AND `deleted` = 0;

-- =============================================
-- 6. 确保工作台本身配置正确
-- =============================================
UPDATE system_menu
SET `component` = '',
    `component_name` = 'Workbench',
    `type` = 1,
    `sort` = -1,
    `update_time` = NOW()
WHERE `id` = @workbench_menu_id AND `deleted` = 0;

-- =============================================
-- 7. 为超级管理员重新分配菜单权限
-- =============================================
SET @weekly_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0 LIMIT 1);
SET @pm_record_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'project-management-record' AND `deleted` = 0 LIMIT 1);
SET @daily_menu_id = (SELECT `id` FROM `system_menu` WHERE `path` = 'daily-management-record' AND `deleted` = 0 LIMIT 1);

SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES 
(@admin_role_id, @workbench_menu_id, '1', NOW(), '1', NOW(), b'0'),
(@admin_role_id, @weekly_menu_id, '1', NOW(), '1', NOW(), b'0'),
(@admin_role_id, @pm_record_menu_id, '1', NOW(), '1', NOW(), b'0'),
(@admin_role_id, @daily_menu_id, '1', NOW(), '1', NOW(), b'0');

-- =============================================
-- 8. 验证清理结果
-- =============================================

SELECT '===== 清理后：工作台结构 =====' AS title;

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
WHERE (m.id = @workbench_menu_id OR m.parent_id = @workbench_menu_id)
  AND m.deleted = 0
ORDER BY 
    CASE WHEN m.id = @workbench_menu_id THEN 0 ELSE 1 END,
    m.sort, m.id;

SELECT CONCAT('清理完成！工作台下应该只有 3 个子菜单。当前数量: ', 
    (SELECT COUNT(*) FROM system_menu WHERE parent_id = @workbench_menu_id AND deleted = 0)
) AS result;

SELECT '请重新登录或 Ctrl+Shift+R 强制刷新页面。' AS note;
