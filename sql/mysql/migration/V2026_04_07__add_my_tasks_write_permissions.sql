-- =============================================
-- 为"我的任务"菜单新增创建/编辑/删除权限按钮
--
-- 问题：普通员工（工程师角色）通过工作台进入轮次详情后，
--       能查看目标和漏洞，但无法添加/编辑/删除，
--       因为写入接口只认 project:info:create/update/delete，
--       而工程师角色只有 project:my-tasks:query。
--
-- 修复：
-- 1. 新增 project:my-tasks:create / update / delete 菜单权限
-- 2. 后端控制器已改为 hasAnyPermissions 同时接受两种权限
-- 3. 将新权限分配给所有角色（含工程师）
-- =============================================

SET NAMES utf8mb4;

-- 获取"我的任务"菜单ID
SET @my_tasks_id = (
    SELECT `id` FROM `system_menu`
    WHERE `path` = 'my-tasks' AND `deleted` = 0
    LIMIT 1
);

SELECT CONCAT('我的任务菜单ID: ', IFNULL(@my_tasks_id, '未找到')) AS debug_info;

-- =============================================
-- 第一步：创建权限按钮（幂等，不重复插入）
-- =============================================

-- project:my-tasks:create
SET @mt_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:create' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的任务创建', 'project:my-tasks:create', 3, 2, @my_tasks_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mt_create_id IS NULL AND @my_tasks_id IS NOT NULL;

SET @mt_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:create' AND `deleted` = 0 LIMIT 1);

-- project:my-tasks:update
SET @mt_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:update' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的任务编辑', 'project:my-tasks:update', 3, 3, @my_tasks_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mt_update_id IS NULL AND @my_tasks_id IS NOT NULL;

SET @mt_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:update' AND `deleted` = 0 LIMIT 1);

-- project:my-tasks:delete
SET @mt_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:delete' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的任务删除', 'project:my-tasks:delete', 3, 4, @my_tasks_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mt_delete_id IS NULL AND @my_tasks_id IS NOT NULL;

SET @mt_delete_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-tasks:delete' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第二步：为各角色分配新权限
-- =============================================

-- 超级管理员
SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` IN (@mt_create_id, @mt_update_id, @mt_delete_id) AND `deleted` = 0;

-- 主管角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@mt_create_id, @mt_update_id, @mt_delete_id);

-- 组长角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_tl', 'ay_tl', 'sh_tl')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@mt_create_id, @mt_update_id, @mt_delete_id);

-- 工程师角色（普通员工，如欧德炜）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (@mt_create_id, @mt_update_id, @mt_delete_id);

-- =============================================
-- 第三步：验证
-- =============================================

SELECT '===== 我的任务权限按钮 =====' AS title;

SELECT
    m.id,
    m.name,
    m.permission,
    CASE m.type WHEN 3 THEN '按钮' ELSE CONCAT('类型', m.type) END AS menu_type,
    m.parent_id
FROM `system_menu` m
WHERE m.permission LIKE 'project:my-tasks:%' AND m.deleted = 0
ORDER BY m.sort;

SELECT '===== 角色权限分配 =====' AS title2;

SELECT r.name AS role_name, r.code AS role_code, m.name AS menu_name, m.permission
FROM `system_role_menu` rm
    JOIN `system_role` r ON r.id = rm.role_id AND r.deleted = 0
    JOIN `system_menu` m ON m.id = rm.menu_id AND m.deleted = 0
WHERE m.permission IN ('project:my-tasks:create', 'project:my-tasks:update', 'project:my-tasks:delete')
  AND rm.deleted = 0
ORDER BY r.code, m.permission;
