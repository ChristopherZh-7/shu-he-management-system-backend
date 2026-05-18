-- =====================================================
-- Migration: V2026_05_18_05__redo_merge_project_menu_after_frontend_rebuilt.sql
-- Date:      2026-05-18
-- Description:
--   重做 V2026_05_18 merge 的菜单合并动作。
--
--   背景：
--     V2026_05_18 merge SQL 把 5080 改成 menu(type=2, path=/project, component=project/index)，
--     与前端 apps/web-antd/src/views/project/index.vue 统一入口对齐。
--     但当时生产前端 dist 是 2026-04-09 的旧版本，没有这个统一入口路由，
--     导致用户访问 /project 时白屏 + console 报 "Component view not found"。
--
--     MCP-1 应急回滚：把 5080 改回 type=1（目录）、5081/5091/5166 改回 deleted=0
--     恢复成原 3 tab 结构。
--
--     现在（V2026_05_18_05 时刻）前端已重新 build + 部署最新 dist，
--     dist 里包含 views/project/index.vue 统一入口路由，
--     可以重新走 V2026_05_18 merge 的设计。
--
--   本 migration 做：
--     1) 把 5080 升级为可点击菜单 type=2, path=/project, component=project/index, component_name=ProjectList
--     2) 软删 5081/5091/5166 三个旧分类子菜单（原本的「安全服务」「数据安全」「安全运营」tab）
--     3) 软删 5081/5091/5166 下挂的按钮（避免与 5080 下已存在的 project:* 按钮重复）
--     4) 隐藏的详情类菜单（5096/5099/5172）保留不动
--
--   注意：5080 下的所有 project:* 按钮（5319-5344，由 V2026_05_18_01/02/04 已补齐）
--   保留不动；超管和历史角色对这些按钮的授权（system_role_menu）也保留不动。
--
--   回滚：把 5080 改回 type=1 + 5081/5091/5166 改回 deleted=0 即可。
-- =====================================================

-- ============================================================
-- 0. 防御：仅当 5080 当前仍是「目录类型」时执行（避免重复执行覆盖）
--    （应急回滚后 5080 type=1，需要重新升级为 type=2）
-- ============================================================
SET @need_redo = (
    SELECT COUNT(*) FROM system_menu
    WHERE id = 5080 AND type = 1 AND deleted = b'0'
);

-- ============================================================
-- 1. 把 5080 升级为可点击菜单（统一项目管理入口）
--    path=/project component=project/index 对应前端 views/project/index.vue
-- ============================================================
UPDATE system_menu
   SET type            = 2,
       path            = '/project',
       component       = 'project/index',
       component_name  = 'ProjectList',
       icon            = 'lucide:folder-kanban',
       visible         = b'1',
       keep_alive      = b'1',
       always_show     = b'0',
       updater         = '1',
       update_time     = NOW()
 WHERE id = 5080
   AND (@need_redo = 1);

-- ============================================================
-- 2. 软删 5081 安全服务、5091 数据安全、5166 安全运营 三个旧分类子菜单
-- ============================================================
UPDATE system_menu
   SET deleted = b'1', updater = '1', update_time = NOW()
 WHERE id IN (5081, 5091, 5166)
   AND deleted = b'0'
   AND (@need_redo = 1);

-- ============================================================
-- 3. 软删 5081/5091/5166 下挂的按钮（避免 permission 重复授权）
--    5080 下已经有 V2026_05_18_01/02/04 补齐的对应 project:* 按钮 (5319-5344)
-- ============================================================
UPDATE system_menu
   SET deleted = b'1', updater = '1', update_time = NOW()
 WHERE parent_id IN (5081, 5091, 5166)
   AND type = 3
   AND deleted = b'0'
   AND (@need_redo = 1);

-- ============================================================
-- 4. 验证
-- ============================================================
SELECT '===== 项目管理菜单重做合并完成 =====' AS msg;

SELECT
    m.id,
    m.name,
    CASE m.type WHEN 1 THEN '目录' WHEN 2 THEN '菜单' WHEN 3 THEN '按钮' END AS menu_type,
    m.parent_id,
    m.path,
    m.component,
    m.component_name,
    CAST(m.deleted AS UNSIGNED) AS deleted
  FROM system_menu m
 WHERE m.id IN (5080, 5081, 5091, 5166)
    OR (m.parent_id = 5080 AND m.type = 2 AND m.deleted = b'0')
 ORDER BY m.deleted, m.type, m.sort, m.id;

SELECT
    CONCAT('5080 当前 type=',
           (SELECT type FROM system_menu WHERE id=5080),
           ' path=',
           (SELECT path FROM system_menu WHERE id=5080),
           ' component=',
           (SELECT component FROM system_menu WHERE id=5080),
           ' (期望 type=2 path=/project component=project/index)'
    ) AS menu5080_check;

SELECT
    CONCAT('5081/5091/5166 仍未删除条数 = ',
           (SELECT COUNT(*) FROM system_menu WHERE id IN (5081,5091,5166) AND deleted = b'0'),
           ' (期望 0)'
    ) AS old_classification_check;

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- UPDATE system_menu SET type=1, path='', component='', component_name='' WHERE id=5080;
-- UPDATE system_menu SET deleted=b'0' WHERE id IN (5081, 5091, 5166);
-- UPDATE system_menu SET deleted=b'0' WHERE parent_id IN (5081, 5091, 5166) AND type=3;
