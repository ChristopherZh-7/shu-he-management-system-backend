-- =============================================
-- 修复詹裕文(226) 安全运营主管权限
--
-- 问题：詹应是安全运营主管(ay_mg)，但被误分配为安全服务(af_*)，
--       导致无法管理 PRJ-2 安全运营项目（如深圳市人民医院 PRJ-2-20260317-5208）
--
-- 本脚本：将詹的角色修正为 ay_mg（安全运营主管），并确保部门归属正确
-- =============================================

SET NAMES utf8mb4;

-- 詹裕文 user_id
SET @zhan_user_id = 226;

-- =============================================
-- 第一步：诊断当前状态
-- =============================================
SELECT '=== 1. 詹裕文当前角色 ===' AS step;
SELECT ur.role_id, r.code, r.name 
FROM system_user_role ur 
JOIN system_role r ON ur.role_id = r.id 
WHERE ur.user_id = @zhan_user_id AND ur.deleted = 0;

SELECT '=== 2. 詹裕文当前部门 ===' AS step;
SELECT u.id, u.nickname, u.dept_id, d.name AS dept_name
FROM system_users u
LEFT JOIN system_dept d ON u.dept_id = d.id AND d.deleted = 0
WHERE u.id = @zhan_user_id;

SELECT '=== 3. 深圳市人民医院项目(PRJ-2-xxx) 归属 ===' AS step;
SELECT p.id, p.code, p.name, p.customer_name, pds.dept_type,
  CASE pds.dept_type WHEN 1 THEN '安全服务' WHEN 2 THEN '安全运营' WHEN 3 THEN '数据安全' END AS dept_type_name
FROM project p
LEFT JOIN project_dept_service pds ON pds.project_id = p.id AND pds.deleted = 0
WHERE (p.code LIKE 'PRJ-2%' OR p.code IS NULL) AND (p.name LIKE '%深圳市人民医院%' OR p.customer_name LIKE '%深圳市人民医院%')
  AND p.deleted = 0;

-- =============================================
-- 第二步：修正角色（移除安全服务角色，添加安全运营主管）
-- =============================================

-- 2.1 移除詹的 安全服务 角色 (af_mg, af_tl, af_emp)
DELETE ur FROM system_user_role ur
INNER JOIN system_role r ON ur.role_id = r.id AND r.deleted = 0
WHERE ur.user_id = @zhan_user_id AND r.code IN ('af_mg', 'af_tl', 'af_emp') AND ur.deleted = 0;

-- 2.2 确保詹拥有 安全运营主管(ay_mg) 角色
INSERT IGNORE INTO system_user_role (user_id, role_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT @zhan_user_id, r.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r
WHERE r.code = 'ay_mg' AND r.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_user_role ur WHERE ur.user_id = @zhan_user_id AND ur.role_id = r.id AND ur.deleted = 0);

-- 2.3 保留 super_admin（若有），不删除其他非 af_* 角色

-- =============================================
-- 第三步：确保詹在安全运营部门（data_scope 本部门及以下 才能看到项目）
-- =============================================
-- 查找安全运营部门（名称含「安全运营」或「运营」的部门，dept_type=2 的 project_dept_service 对应的 dept_id）
-- 若詹的 dept_id 不在安全运营体系下，需在系统「用户管理」中手动调整詹的部门为安全运营相关部门

-- 可选：若已知安全运营部门ID，可执行（请根据实际 system_dept 数据调整）
-- UPDATE system_users SET dept_id = <安全运营部门ID>, updater='1', update_time=NOW() WHERE id = @zhan_user_id;

-- =============================================
-- 第四步：验证
-- =============================================
SELECT '=== 修复后：詹裕文角色 ===' AS step;
SELECT ur.role_id, r.code, r.name 
FROM system_user_role ur 
JOIN system_role r ON ur.role_id = r.id 
WHERE ur.user_id = @zhan_user_id AND ur.deleted = 0;

SELECT 'fix_zhan_security_operation_role 完成。请让詹裕文重新登录后测试管理深圳市人民医院项目。' AS result;
