-- 修复 project_dept_service id=3 报「部门服务单不存在」
-- 原因：project_dept_service 配置了基于 dept_id 的数据权限，id=3 的 dept_id 为 NULL，
-- 导致 dept_id IN (用户部门列表) 无法匹配 NULL，记录被过滤。
--
-- 解决：将 id=3（安全运营 dept_type=2）的 dept_id 设为 124（运营服务部1营1排1班），
-- 詹裕文(user_id=226) 所在部门，使其可访问并设置负责人。

-- 先检查当前数据
SELECT id, project_id, dept_id, dept_name, dept_type, claimed FROM project_dept_service WHERE id = 3;

-- 执行修复（仅 dept_id，dept_name 可后续从 system_dept 同步）
UPDATE project_dept_service SET dept_id = 124 WHERE id = 3 AND dept_id IS NULL;
SELECT id, project_id, dept_id, dept_name FROM project_dept_service WHERE id = 3;

-- 验证
SELECT id, project_id, dept_id, dept_name, dept_type FROM project_dept_service WHERE id = 3;
