-- =============================================
-- 新增"我的任务"菜单（普通员工工作台入口）
--
-- 变更内容：
-- 1. 在工作台下新增"我的任务"子菜单
-- 2. 添加权限按钮
-- 3. 为所有角色（含工程师）分配该菜单权限
-- =============================================

SET NAMES utf8mb4;

-- 获取工作台菜单ID
SET @workbench_id = (
    SELECT `id` FROM `system_menu`
    WHERE (`path` = 'workbench' OR `name` = '工作台')
      AND `deleted` = 0
    LIMIT 1
);

SELECT CONCAT('工作台菜单ID: ', IFNULL(@workbench_id, '未找到')) AS debug_info;

-- =============================================
-- 第一步：创建"我的任务"菜单
-- =============================================

SET @my_tasks_id = (
    SELECT `id` FROM `system_menu`
    WHERE `path` = 'my-tasks' AND `deleted` = 0
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`,
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '我的任务', '', 2, -1, @workbench_id, 'my-tasks',
       'ep:list', 'dashboard/my-tasks/index', 'WorkbenchMyTasks',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @my_tasks_id IS NULL AND @workbench_id IS NOT NULL;

SET @my_tasks_id = COALESCE(
    @my_tasks_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'my-tasks' AND `deleted` = 0 LIMIT 1)
);

-- =============================================
-- 第二步：创建权限按钮
-- =============================================

SET @mt_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:query' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的任务查询', 'project:my-tasks:query', 3, 1, @my_tasks_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mt_query_id IS NULL AND @my_tasks_id IS NOT NULL;

SET @mt_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:query' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第三步：为各角色分配权限
-- =============================================

-- 超级管理员
SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` IN (@my_tasks_id, @mt_query_id) AND `deleted` = 0;

-- 主管角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@my_tasks_id, @mt_query_id);

-- 组长角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_tl', 'ay_tl', 'sh_tl')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@my_tasks_id, @mt_query_id);

-- 工程师角色（这是关键：让普通员工也能看到这个菜单）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@my_tasks_id, @mt_query_id);

-- =============================================
-- 第四步：验证
-- =============================================

SELECT '===== 我的任务菜单 =====' AS title;

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
    END AS menu_type,
    CASE WHEN m.visible = b'1' THEN '可见' ELSE '隐藏' END AS visibility
FROM `system_menu` m
WHERE m.id IN (@my_tasks_id, @mt_query_id)
  AND m.deleted = 0
ORDER BY m.sort, m.id;

SELECT '===== 角色权限分配 =====' AS title2;

SELECT r.name AS role_name, r.code AS role_code, m.name AS menu_name
FROM `system_role_menu` rm
    JOIN `system_role` r ON r.id = rm.role_id AND r.deleted = 0
    JOIN `system_menu` m ON m.id = rm.menu_id AND m.deleted = 0
WHERE rm.menu_id IN (@my_tasks_id, @mt_query_id)
  AND rm.deleted = 0
ORDER BY r.code, m.name;

SELECT '✅ 我的任务菜单创建完成！' AS result;
