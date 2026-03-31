-- =============================================================================
-- DANGEROUS: 清空商机、合同、回款、跟进、权限(业务维度)、项目等业务表
-- 执行前：必须整库备份；且已先执行 BPM cleanModel（见 DANGEROUS_reset_crm_bpm_project_README.txt）
-- 保留：crm_customer、crm_contact、crm_product、crm_product_category、crm_contract_config、
--       crm_clue（线索）、客户池配置等 master/配置（如需连线索一起清，自行取消注释）
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----- CRM：子表先 -----
TRUNCATE TABLE crm_contract_product;
TRUNCATE TABLE crm_receivable_plan;
TRUNCATE TABLE crm_receivable;
TRUNCATE TABLE crm_contract;

TRUNCATE TABLE crm_follow_up_record;
TRUNCATE TABLE crm_contact_business;

-- 商机/合同/回款 等业务对象的数据权限（见 CrmBizTypeEnum：4 商机 5 合同 7 回款 8 回款计划）
DELETE FROM crm_permission WHERE biz_type IN (4, 5, 7, 8);

TRUNCATE TABLE crm_business;

-- 可选：一并清空线索
-- TRUNCATE TABLE crm_clue;

-- ----- 项目模块：子表先（按常见依赖顺序；若某表不存在请删掉该行） -----
TRUNCATE TABLE project_round_vulnerability;
TRUNCATE TABLE project_round_target;
TRUNCATE TABLE project_round;
TRUNCATE TABLE project_member;
TRUNCATE TABLE project_management_record;
TRUNCATE TABLE project_report;
TRUNCATE TABLE project_site_member;
TRUNCATE TABLE project_site;
TRUNCATE TABLE employee_schedule;
TRUNCATE TABLE daily_management_record;
TRUNCATE TABLE project_service_launch_member;
TRUNCATE TABLE project_service_launch;
TRUNCATE TABLE project_dept_service;
TRUNCATE TABLE project_info;
TRUNCATE TABLE security_operation_contract;
TRUNCATE TABLE project;

-- 若仍存在仅依赖项目的扩展表，在此补充 TRUNCATE

-- BPM 扩展：若步骤一已执行 clean，通常已空；保留无害
TRUNCATE TABLE bpm_process_instance_copy;

SET FOREIGN_KEY_CHECKS = 1;

-- 执行后建议：
-- SELECT COUNT(*) FROM crm_business;
-- SELECT COUNT(*) FROM crm_contract;
-- SELECT COUNT(*) FROM project;
