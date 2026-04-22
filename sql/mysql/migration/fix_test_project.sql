-- 修复测试项目 PRJ-20260421-0178 的数据
-- 执行步骤：
-- 1. 先诊断当前成员情况
-- 2. 删除错误的子部门负责人
-- 3. 清理服务项的 deptId（恢复为未分配状态）方便重新测试分配

-- ======= 步骤1：诊断 - 查看项目成员 =======
SELECT pm.id, pm.project_id, pm.user_id, pm.nickname, pm.role_type,
       CASE pm.role_type WHEN 1 THEN '项目经理' WHEN 2 THEN '执行人员' WHEN 3 THEN '审核人员' END as role_name
FROM project_member pm
WHERE pm.project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  AND pm.deleted = 0
ORDER BY pm.role_type, pm.id;

-- ======= 步骤2：删除错误添加的子部门负责人 =======
-- 只保留：roleType=1(项目经理) + 真正的大部门负责人(roleType=3)
-- 删除：roleType=3 中不是大部门负责人的人 + 所有 roleType=2（执行人员，重新由分配流程添加）

-- 找到三个大部门的真正负责人
-- SELECT sd.id, sd.name, sd.dept_type, sd.leader_user_id FROM system_dept sd
-- WHERE sd.dept_type IN (1,2,3) AND sd.deleted = 0 ORDER BY sd.dept_type, sd.id;

-- 删除该项目中 roleType=3 但不是真正大部门负责人的成员
-- 大部门是每个 dept_type 下 ID 最小的那个
UPDATE project_member pm
SET pm.deleted = 1, pm.update_time = NOW()
WHERE pm.project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  AND pm.deleted = 0
  AND pm.role_type = 3
  AND pm.user_id NOT IN (
    -- 保留：每个 dept_type 顶级大部门的负责人
    SELECT sd.leader_user_id
    FROM system_dept sd
    INNER JOIN (
      SELECT dept_type, MIN(id) as min_id
      FROM system_dept
      WHERE dept_type IN (1, 2, 3) AND deleted = 0
      GROUP BY dept_type
    ) top ON sd.id = top.min_id
    WHERE sd.leader_user_id IS NOT NULL
  )
  AND pm.user_id NOT IN (
    -- 保留：同时也是项目经理的人（避免误删）
    SELECT user_id FROM project_member
    WHERE role_type = 1 AND deleted = 0
      AND project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  );

-- 删除所有 roleType=2 的执行人员（将由"分配到班/排"操作重新添加）
UPDATE project_member pm
SET pm.deleted = 1, pm.update_time = NOW()
WHERE pm.project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  AND pm.deleted = 0
  AND pm.role_type = 2;

-- ======= 步骤3：清理服务项的 deptId（重新测试分配） =======
UPDATE project_info
SET dept_id = NULL, update_time = NOW()
WHERE project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  AND deleted = 0
  AND dept_id IS NOT NULL;

-- ======= 步骤4：验证 =======
SELECT pm.id, pm.user_id, pm.nickname, pm.role_type,
       CASE pm.role_type WHEN 1 THEN '项目经理' WHEN 2 THEN '执行人员' WHEN 3 THEN '审核人员' END as role_name
FROM project_member pm
WHERE pm.project_id = (SELECT id FROM project WHERE code = 'PRJ-20260421-0178' AND deleted = 0 LIMIT 1)
  AND pm.deleted = 0
ORDER BY pm.role_type;
