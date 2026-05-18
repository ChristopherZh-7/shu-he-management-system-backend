-- =====================================================
-- Migration: V2026_05_18_02__add_missing_project_site_and_dept_service_permissions.sql
-- Date:      2026-05-18
-- Description:
--   续补 V2026_05_18__merge_project_menu_to_unified_entry.sql 的权限丢失。
--
--   背景：
--     V2026_05_18 merge 把 5081 / 5091 / 5166 三个旧分类菜单及其下挂的全部按钮
--     软删（包括 V2026_03_01_01__add_missing_button_permissions.sql 历史补在
--     5081/5091/5166 下的 `project:site:*` 和 `project:dept-service:*` 按钮），
--     但 merge 脚本只重建了 `project:info:*` / `project:service-item:*` /
--     `project:security-operation:*` 三组 12 个按钮到 5080 下，遗漏了：
--
--     - `project:site:query/create/update/delete`     (ProjectSiteController / ProjectSiteMemberController 使用)
--     - `project:dept-service:query/create/update/delete` (ProjectDeptServiceController 使用)
--
--   V2026_05_18_01__align_project_permission_strings.sql 已经补回了
--   `project:project:*`，但 site / dept-service 这两组仍然缺失，导致：
--     - GET /project/site/list-by-project 整片 403 -> 进项目管理页弹 3 次「没权限」
--     - 部门服务相关接口失权
--
--   本 migration 做两件事：
--     1) 在 5080「项目管理」下新插 8 条按钮（site×4 + dept-service×4）
--     2) 把这 8 条新菜单授权给：
--        a) 超级管理员 role_id=1
--        b) 历史上拥有「5080/5081/5091/5166」下任一 project 按钮权限的所有角色
--           （扫历史授权快照、不在乎源菜单是否已被软删，保证现有用户不掉权限）
--
--   设计原则：与 V2026_05_18_01 完全一致 —— NOT EXISTS 防重、UPDATE 加 deleted=b'0'
--   防误改、@ready 守卫只在 5080 已合并完毕的库上跑、脚本本身幂等可重复执行。
-- =====================================================

-- ============================================================
-- 0. 防御：仅当 5080 已合并到 type=2（已被 V2026_05_18 处理过）才执行
-- ============================================================
SET @ready = (
    SELECT COUNT(*) FROM system_menu
    WHERE id = 5080 AND type = 2 AND deleted = b'0'
);

-- ============================================================
-- 1. 在 5080 下新插 8 条按钮权限
--    sort 接续 V2026_05_18_01 的 1~4（project:project:*）继续往后排
--    （V2026_05_18 merge 已用了 sort 1~12 给 info/service-item/security-operation；
--     这里用 21~28 留出余量，避免与历史排序冲突）
-- ============================================================
INSERT INTO system_menu
       (name,         permission,                       type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT '驻场点查询'   AS name, 'project:site:query'         AS permission, 3 AS type, 21 AS sort, 5080 AS parent_id, '' AS path, '' AS icon, '' AS component, '' AS component_name, 0 AS status, b'1' AS visible, b'1' AS keep_alive, b'0' AS always_show, '1' AS creator, NOW() AS create_time, '1' AS updater, NOW() AS update_time, b'0' AS deleted UNION ALL
    SELECT '驻场点创建',         'project:site:create',         3, 22, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '驻场点更新',         'project:site:update',         3, 23, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '驻场点删除',         'project:site:delete',         3, 24, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '部门服务单查询',     'project:dept-service:query',  3, 25, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '部门服务单创建',     'project:dept-service:create', 3, 26, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '部门服务单更新',     'project:dept-service:update', 3, 27, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '部门服务单删除',     'project:dept-service:delete', 3, 28, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
) AS new_perms
 WHERE @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_menu m
        WHERE m.parent_id = 5080
          AND m.permission = new_perms.permission
          AND m.deleted = b'0'
   );

-- ============================================================
-- 2. 给超级管理员 role_id=1 授权这 8 条新菜单
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
   AND m.permission IN (
       'project:site:query', 'project:site:create', 'project:site:update', 'project:site:delete',
       'project:dept-service:query', 'project:dept-service:create', 'project:dept-service:update', 'project:dept-service:delete'
   )
   AND m.deleted = b'0'
   AND @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id = 1
          AND rm.menu_id = m.id
          AND rm.deleted = b'0'
   );

-- ============================================================
-- 3. 为「历史上拥有 5080/5081/5091/5166 下任一 project 按钮权限」的所有角色
--    自动授权这 8 条新菜单，保证现有用户不掉权限
--    （扫历史授权快照，包含 deleted=1 的菜单，因为 5081/5091/5166 下的菜单已被软删）
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT rm_old.role_id, m_new.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_role_menu rm_old
  JOIN system_menu m_old ON m_old.id = rm_old.menu_id
  JOIN system_menu m_new ON m_new.parent_id = 5080
                        AND m_new.type = 3
                        AND m_new.permission IN (
                            'project:site:query', 'project:site:create', 'project:site:update', 'project:site:delete',
                            'project:dept-service:query', 'project:dept-service:create', 'project:dept-service:update', 'project:dept-service:delete'
                        )
                        AND m_new.deleted = b'0'
 WHERE rm_old.deleted = b'0'
   AND m_old.parent_id IN (5080, 5081, 5091, 5166)
   AND m_old.type = 3
   AND m_old.permission LIKE 'project:%'
   AND @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_role_menu rm_chk
        WHERE rm_chk.role_id = rm_old.role_id
          AND rm_chk.menu_id = m_new.id
          AND rm_chk.deleted = b'0'
   );

-- ============================================================
-- 4. 验证（执行后查看）
-- ============================================================
SELECT '===== project:site:* + project:dept-service:* 权限对齐完成 =====' AS msg;

SELECT
    m.id, m.name, m.permission, m.parent_id, m.sort, CAST(m.deleted AS UNSIGNED) AS deleted
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
 ORDER BY m.deleted, m.sort, m.id;

SELECT
    CONCAT('role_id=1（超管）拿到的 project:site:* 数 = ',
           (SELECT COUNT(*)
              FROM system_role_menu rm
              JOIN system_menu m ON m.id = rm.menu_id
             WHERE rm.role_id = 1
               AND rm.deleted = b'0'
               AND m.deleted = b'0'
               AND m.permission LIKE 'project:site:%'),
           ' 条（期望 4）'
    ) AS super_admin_site_check;

SELECT
    CONCAT('role_id=1（超管）拿到的 project:dept-service:* 数 = ',
           (SELECT COUNT(*)
              FROM system_role_menu rm
              JOIN system_menu m ON m.id = rm.menu_id
             WHERE rm.role_id = 1
               AND rm.deleted = b'0'
               AND m.deleted = b'0'
               AND m.permission LIKE 'project:dept-service:%'),
           ' 条（期望 4）'
    ) AS super_admin_dept_service_check;

SELECT
    CONCAT('全表 project:site:query 未删数 = ',
           (SELECT COUNT(*) FROM system_menu WHERE permission='project:site:query' AND deleted=b'0'),
           ' 条（期望 ≥ 1）'
    ) AS permission_live_check;

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- UPDATE system_menu SET deleted=b'1' WHERE parent_id=5080 AND permission IN (
--     'project:site:query','project:site:create','project:site:update','project:site:delete',
--     'project:dept-service:query','project:dept-service:create','project:dept-service:update','project:dept-service:delete'
-- );
