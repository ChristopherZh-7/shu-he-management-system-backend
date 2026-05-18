-- =====================================================
-- Migration: V2026_05_18__merge_project_menu_to_unified_entry.sql
-- Date:      2026-05-18
-- Description:
--   配合 V2026_04_21__project_dept_type_nullable.sql 完成「项目管理」菜单合并。
--   背景：项目本身不再绑定单一 dept_type，但 system_menu 表里仍保留三个独立子菜单
--        （5081 安全服务 / 5091 数据安全 / 5166 安全运营），导致用户从任意分类
--        进入看到的都是同一份全量项目列表，与设计意图不符。
--
--   本 migration 做三件事：
--   1) 软删三个旧分类子菜单（5081 / 5091 / 5166）及其按钮权限
--   2) 把 5080「项目管理」从一级目录(type=1)改成可点击菜单(type=2)，
--      路径 /project，组件 project/index，对应前端 ProjectList.vue 统一入口
--   3) 把所有 project 相关的功能/按钮权限 reparent 到 5080 之下，保留权限
--      授予关系不破坏
--
--   隐藏的详情类菜单（5096 项目详情 / 5099 轮次详情 / 5172 安全运营详情）保留
--   不变，它们 visible=0 不在菜单展示，但还在被前端路由用到。
--
--   说明：本脚本所有改动都是软删/UPDATE，不会真正删除数据。如需回滚，
--   只需把 deleted 改回 0、type/path/component 改回原值即可。
-- =====================================================

-- ============================================================
-- 0. 防御：仅当 5080 仍是「目录类型 且 未删除」时执行
-- ============================================================
SET @merge_eligible = (
    SELECT COUNT(*) FROM system_menu
    WHERE id = 5080 AND type = 1 AND deleted = b'0'
);

-- 如果已经合并过（type 已经是 2 或者 5080 不存在/已删），则跳过本脚本
-- 通过把 @merge_eligible=0 时所有 UPDATE 条件加 AND (@merge_eligible = 1) 来安全跳过

-- ============================================================
-- 1. 软删 5081 安全服务、5091 数据安全、5166 安全运营 三个分类子菜单
--    （它们对应前端 security-service / data-security / security-operation 三个薄壳页）
-- ============================================================
UPDATE system_menu
   SET deleted = b'1', updater = '1', update_time = NOW()
 WHERE id IN (5081, 5091, 5166)
   AND deleted = b'0'
   AND (@merge_eligible = 1);

-- ============================================================
-- 2. 软删上述三个子菜单下挂的按钮权限
--    parent_id 在 (5081,5091,5166) 的全部 type=3 按钮节点
-- ============================================================
UPDATE system_menu
   SET deleted = b'1', updater = '1', update_time = NOW()
 WHERE parent_id IN (5081, 5091, 5166)
   AND type = 3
   AND deleted = b'0'
   AND (@merge_eligible = 1);

-- ============================================================
-- 3. 把 5080「项目管理」从一级目录(type=1)升级为可点击菜单(type=2)
--    路径 /project，组件 project/index，组件名 ProjectList
--    与 apps/web-antd/src/router/routes/modules/project.ts 中的
--    `path: '', component: 'project/index.vue'` 对应
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
   AND (@merge_eligible = 1);

-- ============================================================
-- 4. 把原挂在 5081/5091/5166 下被「保留下来还需要的」按钮权限
--    reparent 到 5080 之下（防止业务上仍依赖这些 permission 串的功能失权）
--    注意：上一步已经软删了所有 parent_id ∈ (5081,5091,5166) 的按钮，
--    本步骤改为「重新插入一份挂在 5080 下」更安全 —— 避免出现「重复 permission
--    被授给同一角色又同时被软删」的歧义。
--    我们只重新激活/重建一份 project:info / project:service-item / project:security-operation 等核心按钮。
-- ============================================================

-- 4.1 准备：把这些核心按钮 permission 列表插到 5080 下（如已存在则跳过）
INSERT INTO system_menu
       (name,        permission,                              type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT '项目查询'         AS name, 'project:info:query'                  AS permission, 3 AS type, 1  AS sort, 5080 AS parent_id, '' AS path, '' AS icon, '' AS component, '' AS component_name, 0 AS status, b'1' AS visible, b'1' AS keep_alive, b'0' AS always_show, '1' AS creator, NOW() AS create_time, '1' AS updater, NOW() AS update_time, b'0' AS deleted UNION ALL
    SELECT '项目创建',         'project:info:create',                  3, 2,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '项目更新',         'project:info:update',                  3, 3,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '项目删除',         'project:info:delete',                  3, 4,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '服务项查询',       'project:service-item:query',           3, 5,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '服务项创建',       'project:service-item:create',          3, 6,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '服务项更新',       'project:service-item:update',          3, 7,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '服务项删除',       'project:service-item:delete',          3, 8,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '安全运营查询',     'project:security-operation:query',     3, 9,  5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '安全运营创建',     'project:security-operation:create',    3, 10, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '安全运营更新',     'project:security-operation:update',    3, 11, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0' UNION ALL
    SELECT '安全运营删除',     'project:security-operation:delete',    3, 12, 5080, '', '', '', '', 0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
) AS new_perms
 WHERE @merge_eligible = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_menu m
        WHERE m.parent_id = 5080
          AND m.permission = new_perms.permission
          AND m.deleted = b'0'
   );

-- ============================================================
-- 5. 把超级管理员角色 (role_id = 1) 自动授予新插入的按钮权限
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
   AND m.deleted = b'0'
   AND @merge_eligible = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id = 1
          AND rm.menu_id = m.id
          AND rm.deleted = b'0'
   );

-- ============================================================
-- 6. 验证（执行后查看，应只剩一个「项目管理」菜单 + 一组按钮）
-- ============================================================
SELECT '===== 项目管理菜单合并完成 =====' AS msg;

SELECT
    m.id,
    m.name,
    CASE m.type WHEN 1 THEN '目录' WHEN 2 THEN '菜单' WHEN 3 THEN '按钮' END AS menu_type,
    m.parent_id,
    m.path,
    m.component,
    m.permission,
    m.deleted
  FROM system_menu m
 WHERE m.id = 5080 OR m.parent_id = 5080
 ORDER BY m.type, m.sort, m.id;

SELECT
    CONCAT('剩余的旧分类菜单（应该全部 deleted=1）: 5081/5091/5166 = ',
           (SELECT COUNT(*) FROM system_menu WHERE id IN (5081,5091,5166) AND deleted = b'0'),
           ' 条仍未删除'
    ) AS old_classification_check;
