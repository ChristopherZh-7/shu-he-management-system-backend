-- =====================================================
-- Migration: V2026_04_21_02__fix_project_member_duplicates.sql
-- Date: 2026-04-21
-- Description: 修复 V2026_04_21 migration 中错误添加的项目成员
--   原因：migration 的 JOIN 条件 (sd.dept_type = pi.dept_type) 匹配了所有同 dept_type 的部门
--         包括子部门（班/排），导致子部门负责人被错误添加为 roleType=3
--   修复：删除由 migration 添加的子部门负责人（非大部门负责人）
-- =====================================================

-- 诊断：查看被 migration 添加的 roleType=3 成员（creator='1' 且 create_time 为今天）
-- SELECT pm.*, su.nickname, sd.name as dept_name, sd.dept_type, sd.parent_id
-- FROM project_member pm
-- JOIN system_users su ON su.id = pm.user_id
-- JOIN system_dept sd ON sd.leader_user_id = pm.user_id AND sd.deleted = 0
-- WHERE pm.role_type = 3 AND pm.deleted = 0 AND pm.creator = '1'
-- ORDER BY pm.project_id, sd.dept_type;

-- 修复方案：只保留每个 dept_type 中 ID 最小的那个部门（顶级大部门）的负责人
-- 删除其他子部门负责人的 roleType=3 记录

-- 找到每个 dept_type 的顶级大部门ID（ID最小的那个，通常就是最先创建的大部门）
-- 如果你的数据不符合这个规则，请先运行上面的诊断 SQL 确认

-- 删除：role_type=3, 由系统添加(creator='1'), 且该用户所负责的部门不是顶级大部门
DELETE pm FROM project_member pm
WHERE pm.role_type = 3
  AND pm.deleted = 0
  AND pm.creator = '1'
  AND pm.user_id IN (
    -- 子部门负责人：在 system_dept 中有记录，但其所属部门不是该 dept_type 下ID最小的
    SELECT sd.leader_user_id
    FROM system_dept sd
    WHERE sd.dept_type IN (1, 2, 3)
      AND sd.deleted = 0
      AND sd.leader_user_id IS NOT NULL
      AND sd.id NOT IN (
        -- 每个 dept_type 的顶级大部门（ID最小的那个）
        SELECT MIN(id) FROM system_dept
        WHERE dept_type IN (1, 2, 3) AND deleted = 0
        GROUP BY dept_type
      )
  )
  AND pm.user_id NOT IN (
    -- 保护：不删除同时是项目经理(roleType=1)的人（可能身兼多职）
    SELECT user_id FROM project_member
    WHERE role_type = 1 AND deleted = 0
  );

