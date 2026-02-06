-- ============================================================
-- 清空合同模块及服务发起数据
-- 执行时间: 2026-02-05
-- 说明: 清空合同、服务发起相关的所有业务数据，保留表结构
-- 注意: 执行前请确保已完成数据备份！
-- 备份命令（请在MySQL命令行执行）:
-- mysqldump -u [user] -p [database] \
--   crm_contract crm_contract_product crm_contract_config \
--   crm_receivable crm_receivable_plan \
--   contract_dept_allocation service_item_allocation \
--   security_operation_contract security_operation_member \
--   outside_cost_record project_service_launch project_service_launch_member \
--   > contract_backup_20260205.sql
-- ============================================================

SET NAMES utf8mb4;

-- 禁用外键检查（避免删除顺序问题）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一步：清空安全运营相关表（依赖合同分配）
-- ============================================================

-- 清空安全运营成员表
TRUNCATE TABLE security_operation_member;

-- 清空安全运营合同表
TRUNCATE TABLE security_operation_contract;

-- ============================================================
-- 第二步：清空合同分配相关表
-- ============================================================

-- 清空服务项金额分配表（依赖 contract_dept_allocation）
TRUNCATE TABLE service_item_allocation;

-- 清空合同部门分配表
TRUNCATE TABLE contract_dept_allocation;

-- ============================================================
-- 第三步：清空回款相关表（依赖合同）
-- ============================================================

-- 清空回款记录表
TRUNCATE TABLE crm_receivable;

-- 清空回款计划表
TRUNCATE TABLE crm_receivable_plan;

-- ============================================================
-- 第四步：清空服务发起相关表
-- ============================================================

-- 清空服务发起成员表（依赖 project_service_launch）
TRUNCATE TABLE project_service_launch_member;

-- 清空服务发起主表
TRUNCATE TABLE project_service_launch;

-- ============================================================
-- 第五步：清空外出成本记录表（依赖合同/服务发起）
-- ============================================================

-- 清空外出成本记录表
TRUNCATE TABLE outside_cost_record;

-- ============================================================
-- 第六步：清空CRM合同核心表
-- ============================================================

-- 清空合同产品关联表
TRUNCATE TABLE crm_contract_product;

-- 清空合同主表
TRUNCATE TABLE crm_contract;

-- 注意：crm_contract_config 是配置表，一般不需要清空
-- 如需清空，取消下面的注释
-- TRUNCATE TABLE crm_contract_config;

-- ============================================================
-- 第七步：更新关联表中的合同/服务发起引用（将引用置空）
-- ============================================================

-- 清空项目表中的合同引用
UPDATE project SET contract_id = NULL, contract_no = NULL WHERE contract_id IS NOT NULL;

-- 清空项目信息表中的合同引用
UPDATE project_info SET contract_id = NULL, contract_no = NULL, so_contract_id = NULL WHERE contract_id IS NOT NULL OR so_contract_id IS NOT NULL;

-- 清空部门服务表中的合同引用
UPDATE project_dept_service SET contract_id = NULL, contract_no = NULL WHERE contract_id IS NOT NULL;

-- 清空项目轮次表中的服务发起引用
UPDATE project_round SET service_launch_id = NULL WHERE service_launch_id IS NOT NULL;

-- ============================================================
-- 恢复外键检查
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 验证清空结果
-- ============================================================
SELECT 'crm_contract' AS table_name, COUNT(*) AS row_count FROM crm_contract
UNION ALL SELECT 'crm_contract_product', COUNT(*) FROM crm_contract_product
UNION ALL SELECT 'crm_receivable', COUNT(*) FROM crm_receivable
UNION ALL SELECT 'crm_receivable_plan', COUNT(*) FROM crm_receivable_plan
UNION ALL SELECT 'contract_dept_allocation', COUNT(*) FROM contract_dept_allocation
UNION ALL SELECT 'service_item_allocation', COUNT(*) FROM service_item_allocation
UNION ALL SELECT 'security_operation_contract', COUNT(*) FROM security_operation_contract
UNION ALL SELECT 'security_operation_member', COUNT(*) FROM security_operation_member
UNION ALL SELECT 'project_service_launch', COUNT(*) FROM project_service_launch
UNION ALL SELECT 'project_service_launch_member', COUNT(*) FROM project_service_launch_member
UNION ALL SELECT 'outside_cost_record', COUNT(*) FROM outside_cost_record;

SELECT '✅ 合同及服务发起数据清理完成！' AS result;
