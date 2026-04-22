-- =====================================================
-- Migration: V2026_04_21__project_dept_type_nullable.sql
-- Date: 2026-04-21
-- Description: 项目管理多层级权限重构
--   1. project.dept_type 改为可空（新流程中项目不再绑定单一部门类型，由服务项确定归属）
--   2. 确保 project_member 表结构支持新的角色类型
--   3. 修复含 null 的项目编号
-- =====================================================

-- 1. 修改 project 表的 dept_type 允许为 NULL
ALTER TABLE `project`
  MODIFY COLUMN `dept_type` int(11) NULL DEFAULT NULL
  COMMENT '部门类型：1安全服务 2安全运营 3数据安全（项目级可为空，由服务项确定归属）';

-- 2. 修复已有项目编号中包含 null 的记录
UPDATE `project`
  SET `code` = REPLACE(`code`, 'PRJ-null-', 'PRJ-')
  WHERE `code` LIKE 'PRJ-null-%';

-- 3. 清空服务项的 dept_id（新流程中 dept_id 仅在"分配到排/班"时设置）
-- 将已有的 dept_id 清空（这些是创建时自动设置的大部门ID，不是显式分配的）
UPDATE `project_info`
  SET `dept_id` = NULL
  WHERE `dept_id` IS NOT NULL
    AND `deleted` = 0;

-- 4. 为已有项目补充大部门负责人到 project_member
-- 将每个项目中包含的 deptType 对应的大部门负责人加入项目成员
-- 自动添加：如果项目中有某个 deptType 的服务项，就将该 deptType 大部门负责人加入
INSERT INTO `project_member` (`project_id`, `user_id`, `nickname`, `role_type`, `join_time`, `creator`, `updater`, `create_time`, `update_time`, `deleted`)
SELECT DISTINCT pi.project_id, sd.leader_user_id, su.nickname, 3, NOW(), '1', '1', NOW(), NOW(), 0
FROM `project_info` pi
JOIN `system_dept` sd ON sd.dept_type = pi.dept_type AND sd.deleted = 0
JOIN `system_users` su ON su.id = sd.leader_user_id AND su.deleted = 0
WHERE pi.deleted = 0
  AND sd.leader_user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `project_member` pm
    WHERE pm.project_id = pi.project_id
      AND pm.user_id = sd.leader_user_id
      AND pm.deleted = 0
  );

