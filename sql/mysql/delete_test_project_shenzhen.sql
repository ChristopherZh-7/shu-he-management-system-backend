-- ============================================================
-- 删除测试项目：深圳市人民医院 (PRJ-2-20260309-2861)
-- 项目ID: 1
-- 执行时间: 2026-03-16
-- 说明: 删除安全服务页面的测试数据，该服务单已删除但项目仍显示
-- 注意: 执行前请确保已完成数据备份！
-- ============================================================

SET NAMES utf8mb4;

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- 指定要删除的项目ID
SET @project_id = 1;

-- ============================================================
-- 第一步：删除轮次相关子表
-- ============================================================
DELETE prt FROM project_round_target prt
INNER JOIN project_round pr ON prt.round_id = pr.id
WHERE pr.project_id = @project_id;

DELETE prv FROM project_round_vulnerability prv
INNER JOIN project_round pr ON prv.round_id = pr.id
WHERE pr.project_id = @project_id;

-- ============================================================
-- 第二步：删除站点成员（通过 site_id）
-- ============================================================
DELETE psm FROM project_site_member psm
INNER JOIN project_site ps ON psm.site_id = ps.id
WHERE ps.project_id = @project_id;

-- ============================================================
-- 第三步：删除服务启动成员（通过 launch_id）
-- ============================================================
DELETE pslm FROM project_service_launch_member pslm
INNER JOIN project_service_launch psl ON pslm.launch_id = psl.id
WHERE psl.project_id = @project_id;

-- ============================================================
-- 第四步：删除外出请求成员（通过 request_id）
-- ============================================================
DELETE pom FROM project_outside_member pom
INNER JOIN project_outside_request por ON pom.request_id = por.id
WHERE por.project_id = @project_id;

-- ============================================================
-- 第五步：删除中间层表
-- ============================================================
DELETE FROM project_member WHERE project_id = @project_id;
DELETE FROM project_round WHERE project_id = @project_id;
DELETE FROM project_site WHERE project_id = @project_id;
DELETE FROM project_outside_request WHERE project_id = @project_id;
DELETE FROM project_service_launch WHERE project_id = @project_id;
DELETE FROM project_service_execution WHERE project_id = @project_id;
DELETE FROM project_dept_service WHERE project_id = @project_id;
DELETE FROM project_management_record WHERE project_id = @project_id;
-- 以下表可能不存在，如报错可注释掉：
-- DELETE FROM project_work_record WHERE project_id = @project_id;
DELETE FROM project_info WHERE project_id = @project_id;
-- DELETE FROM project_report WHERE project_id = @project_id;
-- DELETE FROM employee_schedule WHERE project_id = @project_id;

-- ============================================================
-- 第七步：删除项目主表
-- ============================================================
DELETE FROM project WHERE id = @project_id;

-- ============================================================
-- 恢复外键检查
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证删除结果
-- ============================================================
SELECT '删除完成' AS result;
SELECT COUNT(*) AS remaining_projects FROM project WHERE id = 1;
