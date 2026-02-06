-- =============================================
-- 修复工作台菜单及角色权限分配
-- 
-- 问题原因：
-- 1. V2026_02_05_06 硬删除了路径包含 'project' 的菜单，导致"项目管理记录"被删
-- 2. V2026_02_05_07 用新的 auto-increment ID 恢复了菜单
-- 3. V2026_02_05_09 删除了 5080-5200 范围的菜单（可能波及工作台 5179 和按钮 5173-5186）
-- 4. V2026_02_02_14 中各角色引用的旧菜单ID（5173-5186, 5179）可能已不存在
-- 5. 项目管理记录和日常管理记录的权限按钮可能丢失
--
-- 修复方案：
-- 1. 确保工作台及其三个子菜单存在且配置正确
-- 2. 确保所有权限按钮存在
-- 3. 为所有角色重新分配工作台相关菜单权限
-- =============================================

SET NAMES utf8mb4;

-- =============================================
-- 第一步：确保工作台菜单存在
-- =============================================

-- 查找工作台
SET @workbench_id = (
    SELECT `id` FROM `system_menu` 
    WHERE (`path` = 'workbench' OR `name` = '工作台') 
      AND `deleted` = 0 
    LIMIT 1
);

-- 如果工作台不存在，创建它
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '工作台', '', 1, -1, 0, 'workbench', 'ep:home-filled', 
       '', 'Workbench', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @workbench_id IS NULL;

-- 重新获取（可能是新创建的）
SET @workbench_id = COALESCE(
    @workbench_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'workbench' AND `deleted` = 0 LIMIT 1)
);

-- 确保工作台配置正确
UPDATE `system_menu`
SET `component` = '',
    `component_name` = 'Workbench',
    `type` = 1,
    `sort` = -1,
    `parent_id` = 0,
    `update_time` = NOW()
WHERE `id` = @workbench_id AND `deleted` = 0;

SELECT CONCAT('工作台菜单ID: ', IFNULL(@workbench_id, '未找到')) AS debug_info;

-- =============================================
-- 第二步：确保周工作日历存在
-- =============================================

SET @weekly_cal_id = (
    SELECT `id` FROM `system_menu` 
    WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0 
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '周工作日历', '', 2, 0, @workbench_id, 'weekly-work-calendar', 
       'ep:calendar', 'dashboard/weekly-work-calendar/index', 'WorkbenchWeeklyWorkCalendar',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @weekly_cal_id IS NULL AND @workbench_id IS NOT NULL;

SET @weekly_cal_id = COALESCE(
    @weekly_cal_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'weekly-work-calendar' AND `deleted` = 0 LIMIT 1)
);

UPDATE `system_menu`
SET `parent_id` = @workbench_id,
    `component` = 'dashboard/weekly-work-calendar/index',
    `component_name` = 'WorkbenchWeeklyWorkCalendar',
    `type` = 2, `sort` = 0,
    `update_time` = NOW()
WHERE `id` = @weekly_cal_id AND `deleted` = 0;

-- 周工作日历查询按钮
SET @weekly_cal_query_id = (
    SELECT `id` FROM `system_menu`
    WHERE `permission` = 'project:weekly-calendar:query' AND `deleted` = 0
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '周工作日历查询', 'project:weekly-calendar:query', 3, 1, @weekly_cal_id, '', '',
       '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @weekly_cal_query_id IS NULL AND @weekly_cal_id IS NOT NULL;

SET @weekly_cal_query_id = COALESCE(
    @weekly_cal_query_id,
    (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:weekly-calendar:query' AND `deleted` = 0 LIMIT 1)
);

-- =============================================
-- 第三步：确保项目管理记录（周报/日报）菜单存在
-- =============================================

SET @pm_record_id = (
    SELECT `id` FROM `system_menu` 
    WHERE `path` = 'project-management-record' AND `deleted` = 0 
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '项目管理记录', '', 2, 1, @workbench_id, 'project-management-record', 
       'ep:document', 'dashboard/work-record/index', 'WorkbenchProjectManagementRecord',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @pm_record_id IS NULL AND @workbench_id IS NOT NULL;

SET @pm_record_id = COALESCE(
    @pm_record_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'project-management-record' AND `deleted` = 0 LIMIT 1)
);

UPDATE `system_menu`
SET `parent_id` = @workbench_id,
    `component` = 'dashboard/work-record/index',
    `component_name` = 'WorkbenchProjectManagementRecord',
    `type` = 2, `sort` = 1,
    `update_time` = NOW()
WHERE `id` = @pm_record_id AND `deleted` = 0;

-- 项目管理记录权限按钮
SET @pmr_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:query' AND `deleted` = 0 LIMIT 1);
SET @pmr_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:create' AND `deleted` = 0 LIMIT 1);
SET @pmr_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:update' AND `deleted` = 0 LIMIT 1);
SET @pmr_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:delete' AND `deleted` = 0 LIMIT 1);
SET @pmr_export_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:export' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目管理记录查询', 'project:management-record:query', 3, 1, @pm_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pmr_query_id IS NULL AND @pm_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目管理记录新增', 'project:management-record:create', 3, 2, @pm_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pmr_create_id IS NULL AND @pm_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目管理记录修改', 'project:management-record:update', 3, 3, @pm_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pmr_update_id IS NULL AND @pm_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目管理记录删除', 'project:management-record:delete', 3, 4, @pm_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pmr_delete_id IS NULL AND @pm_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目管理记录导出', 'project:management-record:export', 3, 5, @pm_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @pmr_export_id IS NULL AND @pm_record_id IS NOT NULL;

-- 重新获取按钮ID
SET @pmr_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:query' AND `deleted` = 0 LIMIT 1);
SET @pmr_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:create' AND `deleted` = 0 LIMIT 1);
SET @pmr_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:update' AND `deleted` = 0 LIMIT 1);
SET @pmr_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:delete' AND `deleted` = 0 LIMIT 1);
SET @pmr_export_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:management-record:export' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第四步：确保日常管理记录菜单存在
-- =============================================

SET @daily_record_id = (
    SELECT `id` FROM `system_menu` 
    WHERE `path` = 'daily-management-record' AND `deleted` = 0 
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, 
    `component`, `component_name`, `status`, `visible`, `keep_alive`, 
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '日常管理记录', '', 2, 2, @workbench_id, 'daily-management-record', 
       'ep:calendar', 'dashboard/daily-management-record/index', 'WorkbenchDailyManagementRecord',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @daily_record_id IS NULL AND @workbench_id IS NOT NULL;

SET @daily_record_id = COALESCE(
    @daily_record_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'daily-management-record' AND `deleted` = 0 LIMIT 1)
);

UPDATE `system_menu`
SET `parent_id` = @workbench_id,
    `component` = 'dashboard/daily-management-record/index',
    `component_name` = 'WorkbenchDailyManagementRecord',
    `type` = 2, `sort` = 2,
    `update_time` = NOW()
WHERE `id` = @daily_record_id AND `deleted` = 0;

-- 日常管理记录权限按钮
SET @dr_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:query' AND `deleted` = 0 LIMIT 1);
SET @dr_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:create' AND `deleted` = 0 LIMIT 1);
SET @dr_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:update' AND `deleted` = 0 LIMIT 1);
SET @dr_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:delete' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '日常记录查询', 'project:daily-record:query', 3, 1, @daily_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @dr_query_id IS NULL AND @daily_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '日常记录创建', 'project:daily-record:create', 3, 2, @daily_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @dr_create_id IS NULL AND @daily_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '日常记录修改', 'project:daily-record:update', 3, 3, @daily_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @dr_update_id IS NULL AND @daily_record_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '日常记录删除', 'project:daily-record:delete', 3, 4, @daily_record_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @dr_delete_id IS NULL AND @daily_record_id IS NOT NULL;

-- 重新获取按钮ID
SET @dr_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:query' AND `deleted` = 0 LIMIT 1);
SET @dr_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:create' AND `deleted` = 0 LIMIT 1);
SET @dr_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:update' AND `deleted` = 0 LIMIT 1);
SET @dr_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:daily-record:delete' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第五步：清理所有角色中失效的工作台相关菜单关联
-- =============================================

-- 删除引用不存在菜单的角色关联（只针对工作台相关菜单）
DELETE FROM `system_role_menu`
WHERE `menu_id` NOT IN (SELECT `id` FROM `system_menu` WHERE `deleted` = 0)
  AND `deleted` = 0;

-- =============================================
-- 第六步：为超级管理员分配工作台菜单权限
-- =============================================

SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` IN (
    @workbench_id, 
    @weekly_cal_id, @weekly_cal_query_id,
    @pm_record_id, @pmr_query_id, @pmr_create_id, @pmr_update_id, @pmr_delete_id, @pmr_export_id,
    @daily_record_id, @dr_query_id, @dr_create_id, @dr_update_id, @dr_delete_id
) AND `deleted` = 0;

-- =============================================
-- 第七步：为所有业务角色分配工作台菜单权限
-- =============================================

-- 主管角色：完整权限（查看 + 新增 + 修改 + 删除 + 导出）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @workbench_id,
    @weekly_cal_id, @weekly_cal_query_id,
    @pm_record_id, @pmr_query_id, @pmr_create_id, @pmr_update_id, @pmr_delete_id, @pmr_export_id,
    @daily_record_id, @dr_query_id, @dr_create_id, @dr_update_id, @dr_delete_id
  );

-- 组长角色：无删除权限
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_tl', 'ay_tl', 'sh_tl')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @workbench_id,
    @weekly_cal_id, @weekly_cal_query_id,
    @pm_record_id, @pmr_query_id, @pmr_create_id, @pmr_update_id, @pmr_export_id,  -- 无 delete
    @daily_record_id, @dr_query_id, @dr_create_id, @dr_update_id                    -- 无 delete
  );

-- 工程师角色：查看 + 新增 + 修改（自己的数据，数据权限控制）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @workbench_id,
    @weekly_cal_id, @weekly_cal_query_id,
    @pm_record_id, @pmr_query_id, @pmr_create_id, @pmr_update_id,  -- 查看+新增+修改
    @daily_record_id, @dr_query_id, @dr_create_id, @dr_update_id   -- 查看+新增+修改
  );

-- =============================================
-- 第八步：验证修复结果
-- =============================================

SELECT '===== 工作台菜单结构 =====' AS title;

SELECT 
    m.id,
    m.name,
    m.path,
    m.permission,
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
WHERE (
    m.id = @workbench_id 
    OR m.parent_id = @workbench_id
    OR m.parent_id IN (
        SELECT id FROM `system_menu` WHERE parent_id = @workbench_id AND deleted = 0
    )
)
AND m.deleted = 0
ORDER BY 
    CASE WHEN m.id = @workbench_id THEN 0 ELSE 1 END,
    m.sort, m.id;

SELECT '===== 各角色工作台菜单数量 =====' AS title;

SELECT 
    r.name AS 角色名称,
    r.code AS 角色编码,
    COUNT(rm.menu_id) AS 工作台菜单数量
FROM `system_role` r
LEFT JOIN `system_role_menu` rm 
    ON r.id = rm.role_id 
    AND rm.deleted = 0
    AND rm.menu_id IN (
        @workbench_id,
        @weekly_cal_id, @weekly_cal_query_id,
        @pm_record_id, @pmr_query_id, @pmr_create_id, @pmr_update_id, @pmr_delete_id, @pmr_export_id,
        @daily_record_id, @dr_query_id, @dr_create_id, @dr_update_id, @dr_delete_id
    )
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg', 'af_tl', 'ay_tl', 'sh_tl', 'af_emp', 'ay_emp', 'sh_emp')
  AND r.deleted = 0
GROUP BY r.id, r.name, r.code
ORDER BY 
    CASE 
        WHEN r.code LIKE '%_mg' THEN 1
        WHEN r.code LIKE '%_tl' THEN 2
        WHEN r.code LIKE '%_emp' THEN 3
    END,
    r.code;

SELECT '✅ 工作台菜单及角色权限修复完成！请重新登录或 Ctrl+Shift+R 强制刷新。' AS result;
