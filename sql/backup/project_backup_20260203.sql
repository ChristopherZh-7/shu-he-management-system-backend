-- ============================================================
-- 项目管理模块数据备份脚本
-- 备份时间: 2026-02-03
-- 备份说明: 清空项目管理数据前的完整备份
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 备份说明：
-- 此脚本仅记录需要备份的表结构，实际数据备份请使用以下命令：
-- 
-- mysqldump -u [username] -p [database] \
--   project \
--   project_info \
--   project_member \
--   project_outside_member \
--   project_outside_request \
--   project_round \
--   project_round_target \
--   project_round_vulnerability \
--   project_site \
--   project_site_member \
--   project_dept_service \
--   project_service_execution \
--   project_service_launch \
--   project_service_launch_member \
--   project_management_record \
--   project_work_record \
--   > sql/backup/project_data_backup_20260203.sql
--
-- 或者使用下面的 SELECT INTO OUTFILE 导出 CSV（需要文件权限）
-- ============================================================

-- 项目管理模块涉及的表清单：
-- 1.  project                        - 主项目表
-- 2.  project_info                   - 项目信息表（旧表）
-- 3.  project_member                 - 项目成员表
-- 4.  project_outside_member         - 外部成员表
-- 5.  project_outside_request        - 外部请求表
-- 6.  project_round                  - 项目轮次表
-- 7.  project_round_target           - 轮次目标表
-- 8.  project_round_vulnerability    - 轮次漏洞表
-- 9.  project_site                   - 项目站点表
-- 10. project_site_member            - 站点成员表
-- 11. project_dept_service           - 项目部门服务表
-- 12. project_service_execution      - 服务执行表
-- 13. project_service_launch         - 服务启动表
-- 14. project_service_launch_member  - 服务启动成员表
-- 15. project_management_record      - 项目管理记录表
-- 16. project_work_record            - 项目工作记录表

-- ============================================================
-- 查询各表当前数据量（执行前请先运行此查询确认数据）
-- ============================================================
/*
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
*/

SET FOREIGN_KEY_CHECKS = 1;
