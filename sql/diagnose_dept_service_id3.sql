-- 诊断部门服务单 id=3
SELECT 'project_dept_service id=3' AS title;
SELECT id, project_id, customer_name, dept_type, dept_name, status, deleted 
FROM project_dept_service WHERE id = 3;

SELECT '深圳市人民医院 的部门服务单' AS title;
SELECT pds.id, pds.project_id, p.name, pds.customer_name, pds.dept_type, pds.dept_name, pds.status, pds.deleted
FROM project_dept_service pds
JOIN project p ON p.id = pds.project_id AND p.deleted = 0
WHERE (p.name LIKE '%深圳市人民医院%' OR pds.customer_name LIKE '%深圳市人民医院%')
  AND pds.deleted = 0
ORDER BY pds.id;
