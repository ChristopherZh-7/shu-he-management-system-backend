-- ============================================================
-- Shuhe Management System - Clean Test Data
-- ============================================================
-- Removes all business/test data while preserving:
--   - System configuration (users, roles, menus, depts, dicts, posts)
--   - BPM/Flowable process definitions
--   - Infrastructure configs and scheduled jobs
--   - DingTalk/notification configurations
--   - CRM product catalog and config tables
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET @start_time = NOW();

SELECT '========== Starting test data cleanup ==========' AS status;

-- ==========================================================
-- 1. Demo tables (completely useless in production)
-- ==========================================================
SELECT '>>> Cleaning demo tables...' AS status;
TRUNCATE TABLE `shuhe_demo01_contact`;
TRUNCATE TABLE `shuhe_demo02_category`;
TRUNCATE TABLE `shuhe_demo03_course`;
TRUNCATE TABLE `shuhe_demo03_grade`;
TRUNCATE TABLE `shuhe_demo03_student`;

-- ==========================================================
-- 2. CRM business data
-- ==========================================================
SELECT '>>> Cleaning CRM business data...' AS status;
TRUNCATE TABLE `crm_business`;
TRUNCATE TABLE `crm_clue`;
TRUNCATE TABLE `crm_contact`;
TRUNCATE TABLE `crm_contact_business`;
TRUNCATE TABLE `crm_contract`;
TRUNCATE TABLE `crm_contract_product`;
TRUNCATE TABLE `crm_customer`;
TRUNCATE TABLE `crm_follow_up_record`;
TRUNCATE TABLE `crm_permission`;
TRUNCATE TABLE `crm_receivable`;
TRUNCATE TABLE `crm_receivable_plan`;
-- KEEP: crm_product, crm_product_category (catalog)
-- KEEP: crm_contract_config, crm_customer_limit_config, crm_customer_pool_config

-- ==========================================================
-- 3. Project data
-- ==========================================================
SELECT '>>> Cleaning project data...' AS status;
TRUNCATE TABLE `project`;
TRUNCATE TABLE `project_dept_service`;
TRUNCATE TABLE `project_info`;
TRUNCATE TABLE `project_management_record`;
TRUNCATE TABLE `project_member`;
TRUNCATE TABLE `project_outside_member`;
TRUNCATE TABLE `project_outside_request`;
TRUNCATE TABLE `project_report`;
TRUNCATE TABLE `project_round`;
TRUNCATE TABLE `project_round_target`;
TRUNCATE TABLE `project_round_vulnerability`;
TRUNCATE TABLE `project_service_execution`;
TRUNCATE TABLE `project_service_launch`;
TRUNCATE TABLE `project_service_launch_member`;
TRUNCATE TABLE `project_site`;
TRUNCATE TABLE `project_site_member`;

-- ==========================================================
-- 4. Contract / Service allocations
-- ==========================================================
SELECT '>>> Cleaning allocation data...' AS status;
TRUNCATE TABLE `contract_dept_allocation`;
TRUNCATE TABLE `service_item_allocation`;

-- ==========================================================
-- 5. Security operations
-- ==========================================================
SELECT '>>> Cleaning security operations...' AS status;
TRUNCATE TABLE `security_operation_contract`;
TRUNCATE TABLE `security_operation_member`;
TRUNCATE TABLE `security_operation_site`;

-- ==========================================================
-- 6. Other business data
-- ==========================================================
SELECT '>>> Cleaning other business data...' AS status;
TRUNCATE TABLE `outside_cost_record`;
TRUNCATE TABLE `daily_management_record`;
TRUNCATE TABLE `employee_schedule`;
TRUNCATE TABLE `ticket_ticket`;
TRUNCATE TABLE `ticket_log`;

-- ==========================================================
-- 7. Flowable runtime + history (keep definitions only)
-- ==========================================================
SELECT '>>> Cleaning Flowable runtime & history...' AS status;

-- Runtime tables
TRUNCATE TABLE `act_ru_actinst`;
TRUNCATE TABLE `act_ru_deadletter_job`;
TRUNCATE TABLE `act_ru_entitylink`;
TRUNCATE TABLE `act_ru_event_subscr`;
TRUNCATE TABLE `act_ru_execution`;
TRUNCATE TABLE `act_ru_external_job`;
TRUNCATE TABLE `act_ru_history_job`;
TRUNCATE TABLE `act_ru_identitylink`;
TRUNCATE TABLE `act_ru_job`;
TRUNCATE TABLE `act_ru_suspended_job`;
TRUNCATE TABLE `act_ru_task`;
TRUNCATE TABLE `act_ru_timer_job`;
TRUNCATE TABLE `act_ru_variable`;

-- History tables
TRUNCATE TABLE `act_hi_actinst`;
TRUNCATE TABLE `act_hi_attachment`;
TRUNCATE TABLE `act_hi_comment`;
TRUNCATE TABLE `act_hi_detail`;
TRUNCATE TABLE `act_hi_entitylink`;
TRUNCATE TABLE `act_hi_identitylink`;
TRUNCATE TABLE `act_hi_procinst`;
TRUNCATE TABLE `act_hi_taskinst`;
TRUNCATE TABLE `act_hi_tsk_log`;
TRUNCATE TABLE `act_hi_varinst`;

-- Event log
TRUNCATE TABLE `act_evt_log`;

-- Clean orphaned bytearrays (variable data, not process definitions)
DELETE FROM `act_ge_bytearray` WHERE `DEPLOYMENT_ID_` IS NULL;

-- KEEP: act_re_deployment, act_re_model, act_re_procdef, act_procdef_info
-- KEEP: act_ge_property, act_id_* (identity)
-- KEEP: flw_* (event definitions)

-- ==========================================================
-- 8. BPM test instance data
-- ==========================================================
SELECT '>>> Cleaning BPM instance data...' AS status;
TRUNCATE TABLE `bpm_oa_leave`;
TRUNCATE TABLE `bpm_process_instance_copy`;
-- KEEP: bpm_category, bpm_form, bpm_process_definition_info
-- KEEP: bpm_process_expression, bpm_process_listener, bpm_user_group

-- ==========================================================
-- 9. Infrastructure & system logs
-- ==========================================================
SELECT '>>> Cleaning logs...' AS status;
TRUNCATE TABLE `infra_api_access_log`;
TRUNCATE TABLE `infra_api_error_log`;
TRUNCATE TABLE `infra_job_log`;
TRUNCATE TABLE `system_login_log`;
TRUNCATE TABLE `system_operate_log`;

-- OAuth2 tokens (will be regenerated on login)
TRUNCATE TABLE `system_oauth2_access_token`;
TRUNCATE TABLE `system_oauth2_refresh_token`;
TRUNCATE TABLE `system_oauth2_code`;
TRUNCATE TABLE `system_oauth2_approve`;

-- Notification/SMS/mail logs
TRUNCATE TABLE `system_notify_message`;
TRUNCATE TABLE `system_sms_code`;
TRUNCATE TABLE `system_sms_log`;
TRUNCATE TABLE `system_mail_log`;
TRUNCATE TABLE `system_dingtalk_notification_log`;
TRUNCATE TABLE `system_dingtalk_robot_message`;

-- KEEP: all system_* config/definition tables
-- KEEP: infra_config, infra_file*, infra_job, infra_codegen_*, infra_data_source_config

-- ==========================================================
-- 10. Code generation test data (optional, uncomment if needed)
-- ==========================================================
-- TRUNCATE TABLE `infra_codegen_column`;
-- TRUNCATE TABLE `infra_codegen_table`;

SET FOREIGN_KEY_CHECKS = 1;

SELECT CONCAT('========== Cleanup complete! Duration: ',
    TIMESTAMPDIFF(SECOND, @start_time, NOW()), 's ==========') AS status;
