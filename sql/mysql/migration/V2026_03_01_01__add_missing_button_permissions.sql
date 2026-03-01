-- =============================================
-- 补充项目管理模块缺失的按钮权限
--
-- 问题：后端控制器使用了 project:dept-service:*、project:project:*、
--       project:site:* 等权限字符串，但 system_menu 表中从未创建这些
--       按钮权限节点。导致即使勾选了"安全服务"等菜单，用户仍无法
--       调用实际的 API 接口。
--
-- 修复：在安全服务(5081)、安全运营(5166)、数据安全(5091)下
--       添加缺失的按钮权限，并自动分配给已拥有父菜单的角色。
-- =============================================

SET NAMES utf8mb4;

-- =============================================
-- 第一步：添加缺失的按钮权限
-- =============================================

-- ---------- 安全服务 (parent_id=5081) ----------

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:query' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单创建', 'project:dept-service:create', 3, 11, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:create' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单更新', 'project:dept-service:update', 3, 12, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:update' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单删除', 'project:dept-service:delete', 3, 13, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:delete' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:query' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目创建', 'project:project:create', 3, 15, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:create' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目更新', 'project:project:update', 3, 16, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:update' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目删除', 'project:project:delete', 3, 17, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:delete' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:query' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:create' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:update' AND `parent_id` = 5081 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, 5081, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:delete' AND `parent_id` = 5081 AND `deleted` = 0);

-- ---------- 安全运营 (parent_id=5166) ----------

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:query' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单创建', 'project:dept-service:create', 3, 11, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:create' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单更新', 'project:dept-service:update', 3, 12, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:update' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单删除', 'project:dept-service:delete', 3, 13, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:delete' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:query' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:query' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:create' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:update' AND `parent_id` = 5166 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, 5166, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:delete' AND `parent_id` = 5166 AND `deleted` = 0);

-- ---------- 数据安全 (parent_id=5091) ----------

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单查询', 'project:dept-service:query', 3, 10, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:query' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单创建', 'project:dept-service:create', 3, 11, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:create' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单更新', 'project:dept-service:update', 3, 12, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:update' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '部门服务单删除', 'project:dept-service:delete', 3, 13, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:dept-service:delete' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '项目详情查询', 'project:project:query', 3, 14, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:project:query' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点查询', 'project:site:query', 3, 18, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:query' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点创建', 'project:site:create', 3, 19, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:create' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点更新', 'project:site:update', 3, 20, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:update' AND `parent_id` = 5091 AND `deleted` = 0);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '驻场点删除', 'project:site:delete', 3, 21, 5091, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'project:site:delete' AND `parent_id` = 5091 AND `deleted` = 0);

-- =============================================
-- 第二步：为已拥有父菜单的角色自动分配新按钮权限
-- 逻辑：如果一个角色已经拥有"安全服务"(5081)菜单，
--       则自动分配 5081 下所有新增的按钮权限给该角色。
--       安全运营(5166)、数据安全(5091) 同理。
-- =============================================

-- 安全服务(5081)下的新按钮 → 分配给已有 5081 的角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_role_menu` rm
JOIN `system_menu` m ON m.parent_id = 5081 AND m.type = 3 AND m.deleted = 0
WHERE rm.menu_id = 5081 AND rm.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0
  );

-- 安全运营(5166)下的新按钮 → 分配给已有 5166 的角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_role_menu` rm
JOIN `system_menu` m ON m.parent_id = 5166 AND m.type = 3 AND m.deleted = 0
WHERE rm.menu_id = 5166 AND rm.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0
  );

-- 数据安全(5091)下的新按钮 → 分配给已有 5091 的角色
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_role_menu` rm
JOIN `system_menu` m ON m.parent_id = 5091 AND m.type = 3 AND m.deleted = 0
WHERE rm.menu_id = 5091 AND rm.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id AND rm2.deleted = 0
  );

-- 超级管理员(role_id=1)确保拥有所有新增按钮
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu` m
WHERE m.type = 3 AND m.deleted = 0
  AND m.parent_id IN (5081, 5166, 5091)
  AND m.permission IN (
    'project:dept-service:query', 'project:dept-service:create',
    'project:dept-service:update', 'project:dept-service:delete',
    'project:project:query', 'project:project:create',
    'project:project:update', 'project:project:delete',
    'project:site:query', 'project:site:create',
    'project:site:update', 'project:site:delete'
  );

-- =============================================
-- 第三步：验证
-- =============================================

SELECT '===== 新增的按钮权限 =====' AS title;

SELECT
    m.id,
    m.name,
    m.permission,
    pm.name AS parent_menu,
    CASE m.type WHEN 3 THEN '按钮' ELSE '其他' END AS menu_type
FROM `system_menu` m
JOIN `system_menu` pm ON pm.id = m.parent_id
WHERE m.type = 3 AND m.deleted = 0
  AND m.permission IN (
    'project:dept-service:query', 'project:dept-service:create',
    'project:dept-service:update', 'project:dept-service:delete',
    'project:project:query', 'project:project:create',
    'project:project:update', 'project:project:delete',
    'project:site:query', 'project:site:create',
    'project:site:update', 'project:site:delete'
  )
ORDER BY m.parent_id, m.sort;

SELECT '===== 角色权限分配情况 =====' AS title2;

SELECT r.name AS role_name, r.code AS role_code, COUNT(rm.menu_id) AS new_permissions
FROM `system_role_menu` rm
JOIN `system_role` r ON r.id = rm.role_id AND r.deleted = 0
JOIN `system_menu` m ON m.id = rm.menu_id AND m.deleted = 0
WHERE rm.deleted = 0
  AND m.permission IN (
    'project:dept-service:query', 'project:dept-service:create',
    'project:dept-service:update', 'project:dept-service:delete',
    'project:project:query', 'project:project:create',
    'project:project:update', 'project:project:delete',
    'project:site:query', 'project:site:create',
    'project:site:update', 'project:site:delete'
  )
GROUP BY r.name, r.code
ORDER BY r.code;

SELECT '完成！请重新登录后生效。' AS result;
