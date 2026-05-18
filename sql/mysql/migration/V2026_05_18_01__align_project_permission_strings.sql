-- =====================================================
-- Migration: V2026_05_18_01__align_project_permission_strings.sql
-- Date:      2026-05-18
-- Description:
--   修复 V2026_05_18__merge_project_menu_to_unified_entry.sql 留下的权限串错位问题。
--
--   背景：
--     前一版 migration 在 5080「项目管理」下重建了 4 条按钮权限串为 `project:info:*`
--     （`project:info:query/create/update/delete`），但 `ProjectController` 上的
--     `@PreAuthorize` 全部使用的是 `project:project:*` 权限串。
--
--   现状（修复前）：
--     - DB 中 `project:project:query` 唯一的 3 个记录全部 deleted=1（之前挂在 5081/5091/5166 下，被合并 migration 软删）
--     - role_id=1（超管）拥有的 `project:project:*` 菜单 = 0 条
--     - 超管能通过 `hasAnyPermissions` 的「超管兜底」分支访问接口
--     - 但所有非超管角色 + 所有前端按钮（auth: ['project:project:*']）都失权
--
--   本 migration 做三件事：
--     1) 软删 V2026_05_18 错插的 4 条 `project:info:*` 死权限（仅这 4 条，
--        `project:service-item:*` 和 `project:security-operation:*` 是 ServiceItemController
--        实际使用的权限串，保留不动）
--     2) 在 5080 下新插 4 条与后端代码对齐的 `project:project:*` 权限
--     3) 把新权限授权给：
--        - 超级管理员 role_id=1
--        - 历史上拥有「5080/5081/5091/5166」下任一按钮权限的所有角色（不掉权限）
--
--   设计原则：所有 INSERT 都用 NOT EXISTS 防重；UPDATE 都加 deleted=b'0' 防止改到已删
--   数据；脚本本身是幂等的，重复执行无副作用。如需回滚见文件末。
-- =====================================================

-- ============================================================
-- 0. 防御：仅当 5080 已合并到 type=2（已经被前一版 migration 处理过）才执行
-- ============================================================
SET @ready = (
    SELECT COUNT(*) FROM system_menu
    WHERE id = 5080 AND type = 2 AND deleted = b'0'
);

-- ============================================================
-- 1. 软删 V2026_05_18 错插的 4 条 project:info:* 死权限
--    （仅匹配 parent_id=5080 + permission=project:info:* 的，避免误删历史数据）
-- ============================================================
UPDATE system_menu
   SET deleted = b'1', updater = '1', update_time = NOW()
 WHERE parent_id = 5080
   AND type = 3
   AND permission IN (
       'project:info:query',
       'project:info:create',
       'project:info:update',
       'project:info:delete'
   )
   AND deleted = b'0'
   AND (@ready = 1);

-- ============================================================
-- 2. 在 5080 下新插 4 条 project:project:* 权限（与 ProjectController 对齐）
--    sort 沿用 1~4，保持顺序
-- ============================================================
INSERT INTO system_menu
       (name,         permission,                type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT '项目查询'   AS name, 'project:project:query'  AS permission, 3 AS type, 1 AS sort, 5080 AS parent_id, '' AS path, '' AS icon, '' AS component, '' AS component_name, 0 AS status, b'1' AS visible, b'1' AS keep_alive, b'0' AS always_show, '1' AS creator, NOW() AS create_time, '1' AS updater, NOW() AS update_time, b'0' AS deleted UNION ALL
    SELECT '项目创建',         'project:project:create', 3, 2, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '项目更新',         'project:project:update', 3, 3, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '项目删除',         'project:project:delete', 3, 4, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
) AS new_perms
 WHERE @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_menu m
        WHERE m.parent_id = 5080
          AND m.permission = new_perms.permission
          AND m.deleted = b'0'
   );

-- ============================================================
-- 3. 给超级管理员 role_id=1 授权这 4 条新菜单
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
   AND m.permission IN (
       'project:project:query',
       'project:project:create',
       'project:project:update',
       'project:project:delete'
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
-- 4. 为「历史上拥有 5080/5081/5091/5166 下任一 project 按钮权限」的所有角色
--    自动授权这 4 条新菜单，保证现有用户不掉权限
--    （扫历史授权快照，包含 deleted=1 的菜单，因为 5081/5091/5166 下的菜单已被软删）
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT rm_old.role_id, m_new.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_role_menu rm_old
  JOIN system_menu m_old ON m_old.id = rm_old.menu_id
  JOIN system_menu m_new ON m_new.parent_id = 5080
                        AND m_new.type = 3
                        AND m_new.permission IN (
                            'project:project:query',
                            'project:project:create',
                            'project:project:update',
                            'project:project:delete'
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
-- 5. 验证（执行后查看）
-- ============================================================
SELECT '===== project:project:* 权限对齐完成 =====' AS msg;

SELECT
    m.id, m.name, m.permission, m.parent_id, m.sort, CAST(m.deleted AS UNSIGNED) AS deleted
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
 ORDER BY m.deleted, m.sort, m.id;

SELECT
    CONCAT('role_id=1（超管）拿到的 project:project:* 数 = ',
           (SELECT COUNT(*)
              FROM system_role_menu rm
              JOIN system_menu m ON m.id = rm.menu_id
             WHERE rm.role_id = 1
               AND rm.deleted = b'0'
               AND m.deleted = b'0'
               AND m.permission LIKE 'project:project:%'),
           ' 条（期望 4）'
    ) AS super_admin_check;

SELECT
    CONCAT('全表 project:project:query 未删数 = ',
           (SELECT COUNT(*) FROM system_menu WHERE permission='project:project:query' AND deleted=b'0'),
           ' 条（期望 ≥ 1）'
    ) AS permission_live_check;

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- UPDATE system_menu SET deleted=b'0' WHERE parent_id=5080 AND permission IN ('project:info:query','project:info:create','project:info:update','project:info:delete');
-- UPDATE system_menu SET deleted=b'1' WHERE parent_id=5080 AND permission IN ('project:project:query','project:project:create','project:project:update','project:project:delete');
