-- =============================================
-- 诊断用户 249 在安全服务页面的编辑权限问题
-- =============================================

SET NAMES utf8mb4;

-- 1. 用户 249 基本信息
SELECT '===== 1. 用户 249 基本信息 =====' AS title;
SELECT id, username, nickname, dept_id, status, create_time
FROM system_users WHERE id = 249 AND deleted = 0;

-- 2. 用户 249 的角色
SELECT '===== 2. 用户 249 的角色 =====' AS title;
SELECT ur.user_id, ur.role_id, r.name AS role_name, r.code AS role_code
FROM system_user_role ur
JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0
WHERE ur.user_id = 249 AND ur.deleted = 0;

-- 3. 安全服务菜单 (path=security-service)
SELECT '===== 3. 安全服务菜单 =====' AS title;
SELECT id, name, path, permission, type, parent_id
FROM system_menu
WHERE path = 'security-service' AND deleted = 0
   OR (parent_id IN (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0) AND deleted = 0)
ORDER BY parent_id, sort;

-- 4. 安全服务下需要的按钮权限（编辑相关）
SELECT '===== 4. 安全服务下编辑相关按钮权限 =====' AS title;
SELECT m.id, m.name, m.permission, m.parent_id, pm.name AS parent_name
FROM system_menu m
LEFT JOIN system_menu pm ON pm.id = m.parent_id
WHERE m.deleted = 0 AND m.type = 3
  AND m.permission IN (
    'project:dept-service:query', 'project:dept-service:update',
    'project:project:query', 'project:project:update',
    'project:service-item:query', 'project:service-item:create',
    'project:service-item:update', 'project:service-item:delete',
    'project:site:query', 'project:site:update'
  )
  AND m.parent_id IN (SELECT id FROM system_menu WHERE path IN ('security-service','security-operation','data-security') AND deleted = 0)
ORDER BY m.parent_id, m.permission;

-- 5. 用户 249 通过角色拥有的菜单/权限（含安全服务相关）
SELECT '===== 5. 用户249 拥有的安全服务相关权限 =====' AS title;
SELECT DISTINCT m.id, m.name, m.permission, m.type
FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 249 AND ur.deleted = 0
  AND (m.path = 'security-service'
       OR m.parent_id IN (SELECT id FROM system_menu WHERE path = 'security-service' AND deleted = 0)
       OR m.permission IN ('project:project:update','project:service-item:update','project:dept-service:query'))
ORDER BY m.type, m.permission;

-- 6. 用户 249 是否拥有 project:project:update
SELECT '===== 6. 用户249 是否拥有 project:project:update =====' AS title;
SELECT COUNT(*) AS has_project_update
FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 249 AND ur.deleted = 0 AND rm.deleted = 0
  AND m.permission = 'project:project:update';

-- 7. 用户 249 是否拥有 project:service-item:update
SELECT '===== 7. 用户249 是否拥有 project:service-item:update =====' AS title;
SELECT COUNT(*) AS has_service_item_update
FROM system_user_role ur
JOIN system_role_menu rm ON rm.role_id = ur.role_id AND rm.deleted = 0
JOIN system_menu m ON m.id = rm.menu_id AND m.deleted = 0
WHERE ur.user_id = 249 AND ur.deleted = 0 AND rm.deleted = 0
  AND m.permission = 'project:service-item:update';

-- 8. 安全服务(5081) 或 path=security-service 下的 project:project:update 和 project:service-item:update 菜单是否存在
SELECT '===== 8. 安全服务下 project:project:update / project:service-item:update 菜单 =====' AS title;
SELECT m.id, m.name, m.permission, m.parent_id, pm.path AS parent_path
FROM system_menu m
JOIN system_menu pm ON pm.id = m.parent_id AND pm.deleted = 0
WHERE m.deleted = 0 AND m.type = 3
  AND m.permission IN ('project:project:update', 'project:service-item:update')
  AND (pm.path = 'security-service' OR pm.id = 5081);
