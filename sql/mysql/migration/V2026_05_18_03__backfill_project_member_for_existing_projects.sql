-- =====================================================
-- Migration: V2026_05_18_03__backfill_project_member_for_existing_projects.sql
-- Date:      2026-05-18
-- Description:
--   补回历史项目缺失的 project_member 记录。
--
--   背景：
--     ProjectServiceImpl.createProject 历史上只 insert 了 project 主表，
--     没有把「创建者」和「managerIds 里的项目负责人」插入 project_member 表。
--     ProjectServiceImpl.updateProject 改 managerIds 时也只调了钉钉加群，
--     同样没 insert project_member。
--
--     结果（V2026_05_18 同代码已修复后续新建项目）：
--     - 非超管用户创建的项目，自己也查不到 roleType → /project/manage/{id} 弹「无权限」
--     - 非超管用户在「项目分页」中看不到自己创建/负责的项目
--     - 这是数据 bug 不是权限串问题，所以即便 V2026_05_18_01/02 已经把按钮权限全补给角色，
--       仍然会因为 project_member 缺记录而报错
--
--   本 migration 做两件事：
--     1) 为「creator 是合法数字 user_id」的所有存量项目，把 creator 补成 roleType=1 项目经理
--     2) 为「manager_ids JSON 数组非空」的所有存量项目，把 manager_ids 里的每个 user 补成 roleType=1 项目经理
--
--   设计原则：
--     - 不修改任何已有 project_member 记录（NOT EXISTS 防重）
--     - 不删除任何记录
--     - 沿用 V2026_05_18_02 风格：b'0' bit 写法、'1' 当系统 creator、可重复执行幂等
--     - tenant_id 继承自 project.tenant_id 避免跨租户串数据
--     - JSON_TABLE 是 MySQL 8.0+ 功能，本项目 V2026_01_29_02 / V2026_02_03_04 已用 JSON 类型，
--       说明库版本 ≥ 8.0
-- =====================================================

-- ============================================================
-- 1. 把 project.creator 补成项目经理（roleType=1）
--    - 只处理 creator 是纯数字的（排除 'admin' 这类历史字符串）
--    - 只处理 project 未删
--    - 跳过同 (project_id, user_id) 已经存在 project_member 的（不论是否已删除）
-- ============================================================
INSERT INTO project_member
       (project_id, user_id, nickname, role_type, join_time,
        creator,    create_time, updater, update_time, deleted, tenant_id)
SELECT p.id                                       AS project_id,
       CAST(p.creator AS UNSIGNED)                AS user_id,
       COALESCE(u.nickname, '')                   AS nickname,
       1                                          AS role_type,
       p.create_time                              AS join_time,
       '1'                                        AS creator,
       NOW()                                      AS create_time,
       '1'                                        AS updater,
       NOW()                                      AS update_time,
       b'0'                                       AS deleted,
       p.tenant_id                                AS tenant_id
  FROM project p
  LEFT JOIN system_users u ON u.id = CAST(p.creator AS UNSIGNED)
 WHERE p.deleted = b'0'
   AND p.creator IS NOT NULL
   AND p.creator <> ''
   AND p.creator REGEXP '^[0-9]+$'
   AND NOT EXISTS (
       SELECT 1
         FROM project_member m
        WHERE m.project_id = p.id
          AND m.user_id    = CAST(p.creator AS UNSIGNED)
   );

-- ============================================================
-- 2. 把 project.manager_ids JSON 数组里的所有 user 补成项目经理（roleType=1）
--    - 跳过 manager_ids 为 NULL / 空数组的项目
--    - 跳过同 (project_id, user_id) 已经存在 project_member 的
--    - 用 JSON_TABLE 展开 manager_ids 数组（MySQL 8.0+）
-- ============================================================
INSERT INTO project_member
       (project_id, user_id, nickname, role_type, join_time,
        creator,    create_time, updater, update_time, deleted, tenant_id)
SELECT p.id                                       AS project_id,
       jt.manager_id                              AS user_id,
       COALESCE(u.nickname, '')                   AS nickname,
       1                                          AS role_type,
       p.create_time                              AS join_time,
       '1'                                        AS creator,
       NOW()                                      AS create_time,
       '1'                                        AS updater,
       NOW()                                      AS update_time,
       b'0'                                       AS deleted,
       p.tenant_id                                AS tenant_id
  FROM project p
  JOIN JSON_TABLE(
        p.manager_ids,
        '$[*]' COLUMNS (manager_id BIGINT PATH '$')
       ) jt ON jt.manager_id IS NOT NULL
  LEFT JOIN system_users u ON u.id = jt.manager_id
 WHERE p.deleted = b'0'
   AND p.manager_ids IS NOT NULL
   AND JSON_LENGTH(p.manager_ids) > 0
   AND NOT EXISTS (
       SELECT 1
         FROM project_member m
        WHERE m.project_id = p.id
          AND m.user_id    = jt.manager_id
   );

-- ============================================================
-- 3. 验证（执行后查看）
-- ============================================================
SELECT '===== project_member 回填完成 =====' AS msg;

-- 3.1 总览：每个项目当前 project_member 数 vs 期望（creator + distinct managerIds）
SELECT
    p.id                                           AS project_id,
    p.name                                         AS project_name,
    p.creator                                      AS creator_user_id,
    JSON_LENGTH(COALESCE(p.manager_ids, JSON_ARRAY())) AS manager_count,
    (SELECT COUNT(*)
       FROM project_member m
      WHERE m.project_id = p.id
        AND m.deleted    = b'0'
        AND m.role_type  = 1
    )                                              AS pm_manager_rows
  FROM project p
 WHERE p.deleted = b'0'
 ORDER BY p.id;

-- 3.2 反向检查：是否还有「creator 是合法数字 user_id 但 project_member 里没记录」的项目
SELECT
    p.id, p.name, p.creator
  FROM project p
 WHERE p.deleted = b'0'
   AND p.creator REGEXP '^[0-9]+$'
   AND NOT EXISTS (
       SELECT 1 FROM project_member m
        WHERE m.project_id = p.id
          AND m.user_id    = CAST(p.creator AS UNSIGNED)
          AND m.deleted    = b'0'
   );

-- 3.3 zhengyi (user_id=249) 现在能看到的项目数（验你本人那条 case）
SELECT
    CONCAT('user_id=249 (zhengyi) 参与的项目数 = ',
           (SELECT COUNT(DISTINCT m.project_id)
              FROM project_member m
              JOIN project p ON p.id = m.project_id
             WHERE m.user_id = 249
               AND m.deleted = b'0'
               AND p.deleted = b'0'),
           ' 个'
    ) AS zhengyi_visible_check;

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- 物理删除本次 migration 写入的 project_member 行（按 creator='1' + create_time 区分）
-- 注意：会把今天由本脚本插的所有 roleType=1 记录全部物理删掉，老数据不动
-- DELETE FROM project_member
--  WHERE creator = '1'
--    AND role_type = 1
--    AND create_time >= CURDATE()
--    AND create_time <  CURDATE() + INTERVAL 1 DAY;
