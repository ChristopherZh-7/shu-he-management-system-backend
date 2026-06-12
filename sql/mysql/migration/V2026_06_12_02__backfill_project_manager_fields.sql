-- =====================================================
-- Migration: V2026_06_12_02__backfill_project_manager_fields.sql
-- Date: 2026-06-12
-- Description: 回填 project.manager_ids / manager_names 冗余列
--   背景：早期「商机审批通过自动创建项目」的代码只写 project_member
--         （role_type=1 项目经理），未同步写 project.manager_ids/manager_names；
--         前端项目列表「项目负责人」列读的是后者，导致历史项目负责人显示为空。
--         现行代码（CrmBusinessServiceImpl.createProjectInternally）已同步写入，
--         本迁移仅修复历史存量数据。
--   口径：以 project_member 中 role_type=1 且未删除的成员为准，
--         昵称优先取 system_users 当前昵称，其次取 project_member 冗余昵称。
--   幂等：仅回填 manager_ids 为 NULL 或空数组的项目，可重复执行。
-- =====================================================

UPDATE project p
JOIN (
  SELECT t.project_id,
         JSON_ARRAYAGG(t.user_id)  AS ids,
         JSON_ARRAYAGG(t.nickname) AS names
  FROM (
    SELECT pm.project_id,
           pm.user_id,
           COALESCE(u.nickname, pm.nickname, CONCAT('用户', pm.user_id)) AS nickname
    FROM project_member pm
    LEFT JOIN system_users u ON u.id = pm.user_id AND u.deleted = 0
    WHERE pm.role_type = 1
      AND pm.deleted = 0
    GROUP BY pm.project_id, pm.user_id,
             COALESCE(u.nickname, pm.nickname, CONCAT('用户', pm.user_id))
  ) t
  GROUP BY t.project_id
) m ON m.project_id = p.id
SET p.manager_ids   = m.ids,
    p.manager_names = m.names,
    p.update_time   = NOW()
WHERE p.deleted = 0
  AND (p.manager_ids IS NULL OR JSON_LENGTH(p.manager_ids) = 0);

-- 验证
SELECT '===== 回填后仍无负责人的未删除项目（期望仅剩确实没有 role_type=1 成员的项目） =====' AS msg;
SELECT p.id, p.code, p.name
FROM project p
WHERE p.deleted = 0
  AND (p.manager_ids IS NULL OR JSON_LENGTH(p.manager_ids) = 0);
