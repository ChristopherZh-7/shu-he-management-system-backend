-- ============================================================
-- 清空项目管理模块数据
-- 执行时间: 2026-02-03
-- 说明: 清空项目管理相关的所有业务数据，保留表结构
-- 注意: 执行前请确保已完成数据备份！
-- 备份命令: mysqldump -u [user] -p [db] project project_info project_member 
--          project_outside_member project_outside_request project_round 
--          project_round_target project_round_vulnerability project_site 
--          project_site_member project_dept_service project_service_execution 
--          project_service_launch project_service_launch_member 
--          project_management_record project_work_record > backup.sql
-- ============================================================

SET NAMES utf8mb4;

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一步：清空子表（有外键依赖的表）
-- ============================================================

-- 清空轮次相关子表
TRUNCATE TABLE project_round_target;
TRUNCATE TABLE project_round_vulnerability;

-- 清空站点成员表
TRUNCATE TABLE project_site_member;

-- 清空服务启动成员表
TRUNCATE TABLE project_service_launch_member;

-- 清空外部成员表
TRUNCATE TABLE project_outside_member;

-- 清空项目成员表
TRUNCATE TABLE project_member;

-- ============================================================
-- 第二步：清空中间层表
-- ============================================================

-- 清空项目轮次表
TRUNCATE TABLE project_round;

-- 清空项目站点表
TRUNCATE TABLE project_site;

-- 清空外部请求表
TRUNCATE TABLE project_outside_request;

-- 清空服务启动表
TRUNCATE TABLE project_service_launch;

-- 清空服务执行表
TRUNCATE TABLE project_service_execution;

-- 清空部门服务表
TRUNCATE TABLE project_dept_service;

-- 清空项目管理记录表
TRUNCATE TABLE project_management_record;

-- 清空项目工作记录表
TRUNCATE TABLE project_work_record;

-- ============================================================
-- 第三步：清空主表
-- ============================================================

-- 清空项目信息表（旧表，可能已废弃）
TRUNCATE TABLE project_info;

-- 清空主项目表
TRUNCATE TABLE project;

-- ============================================================
-- 恢复外键检查
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证清空结果
-- ============================================================
SELECT 'project' AS table_name, COUNT(*) AS row_count FROM project
UNION ALL SELECT 'project_info', COUNT(*) FROM project_info
UNION ALL SELECT 'project_member', COUNT(*) FROM project_member
UNION ALL SELECT 'project_outside_member', COUNT(*) FROM project_outside_member
UNION ALL SELECT 'project_outside_request', COUNT(*) FROM project_outside_request
UNION ALL SELECT 'project_round', COUNT(*) FROM project_round
UNION ALL SELECT 'project_round_target', COUNT(*) FROM project_round_target
UNION ALL SELECT 'project_round_vulnerability', COUNT(*) FROM project_round_vulnerability
UNION ALL SELECT 'project_site', COUNT(*) FROM project_site
UNION ALL SELECT 'project_site_member', COUNT(*) FROM project_site_member
UNION ALL SELECT 'project_dept_service', COUNT(*) FROM project_dept_service
UNION ALL SELECT 'project_service_execution', COUNT(*) FROM project_service_execution
UNION ALL SELECT 'project_service_launch', COUNT(*) FROM project_service_launch
UNION ALL SELECT 'project_service_launch_member', COUNT(*) FROM project_service_launch_member
UNION ALL SELECT 'project_management_record', COUNT(*) FROM project_management_record
UNION ALL SELECT 'project_work_record', COUNT(*) FROM project_work_record;
