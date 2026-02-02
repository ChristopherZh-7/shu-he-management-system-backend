/*
 生产环境数据库初始化脚本
 
 使用说明：
 1. 此脚本用于清空开发/测试数据，保留系统必要的基础数据
 2. 请在全新数据库或备份后执行
 3. 执行前请先导入 shuhe-ms.sql 创建表结构
 4. 执行此脚本后，需要修改管理员密码

 执行顺序：
 1. 先执行 shuhe-ms.sql（创建表结构 + 基础数据）
 2. 再执行本脚本（清理测试数据）
 
 Date: 2026-02-02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 第一部分：清空日志类表（这些表在生产环境需要清空）
-- =====================================================

-- 清空 API 访问日志
TRUNCATE TABLE `infra_api_access_log`;

-- 清空 API 错误日志
TRUNCATE TABLE `infra_api_error_log`;

-- 清空定时任务日志
TRUNCATE TABLE `infra_job_log`;

-- 清空登录日志
TRUNCATE TABLE `system_login_log`;

-- 清空操作日志
TRUNCATE TABLE `system_operate_log`;

-- 清空邮件发送日志
TRUNCATE TABLE `system_mail_log`;

-- 清空短信发送日志
TRUNCATE TABLE `system_sms_log`;

-- 清空短信验证码
TRUNCATE TABLE `system_sms_code`;

-- 清空站内信消息
TRUNCATE TABLE `system_notify_message`;

-- =====================================================
-- 第二部分：清空代码生成相关表
-- =====================================================

-- 清空代码生成表配置
TRUNCATE TABLE `infra_codegen_table`;

-- 清空代码生成列配置
TRUNCATE TABLE `infra_codegen_column`;

-- =====================================================
-- 第三部分：清空文件相关表
-- =====================================================

-- 清空文件表
TRUNCATE TABLE `infra_file`;

-- 清空文件内容表
TRUNCATE TABLE `infra_file_content`;

-- =====================================================
-- 第四部分：清空 OAuth2 相关表（Token、授权码等）
-- =====================================================

-- 清空 OAuth2 访问令牌
TRUNCATE TABLE `system_oauth2_access_token`;

-- 清空 OAuth2 刷新令牌
TRUNCATE TABLE `system_oauth2_refresh_token`;

-- 清空 OAuth2 授权码
TRUNCATE TABLE `system_oauth2_code`;

-- 清空 OAuth2 授权同意
TRUNCATE TABLE `system_oauth2_approve`;

-- =====================================================
-- 第五部分：清空社交登录相关表
-- =====================================================

-- 清空社交用户表
TRUNCATE TABLE `system_social_user`;

-- 清空社交用户绑定表
TRUNCATE TABLE `system_social_user_bind`;

-- =====================================================
-- 第六部分：清空通知公告
-- =====================================================

-- 清空通知公告
TRUNCATE TABLE `system_notice`;

-- =====================================================
-- 第七部分：删除演示/测试表
-- =====================================================

-- 删除演示模块表（生产环境不需要）
DROP TABLE IF EXISTS `shuhe_demo01_contact`;
DROP TABLE IF EXISTS `shuhe_demo02_category`;
DROP TABLE IF EXISTS `shuhe_demo03_course`;
DROP TABLE IF EXISTS `shuhe_demo03_grade`;
DROP TABLE IF EXISTS `shuhe_demo03_student`;

-- =====================================================
-- 第七部分B：清空业务模块测试数据（根据需要取消注释）
-- =====================================================

-- ============ 项目管理模块 ============
-- 如果需要清空项目测试数据，取消以下注释
-- TRUNCATE TABLE `project_info`;
-- TRUNCATE TABLE `project_member`;
-- TRUNCATE TABLE `project_round`;
-- TRUNCATE TABLE `project_round_detail`;
-- TRUNCATE TABLE `project_work_record`;
-- TRUNCATE TABLE `project_daily_record`;

-- ============ 服务执行模块 ============
-- TRUNCATE TABLE `service_execution`;
-- TRUNCATE TABLE `service_execution_attachment`;
-- TRUNCATE TABLE `service_launch`;
-- TRUNCATE TABLE `service_launch_delegation`;
-- TRUNCATE TABLE `service_launch_member`;

-- ============ 安全运维模块 ============
-- TRUNCATE TABLE `security_operation`;
-- TRUNCATE TABLE `security_operation_site`;

-- ============ 合同分配模块 ============
-- TRUNCATE TABLE `contract_allocation`;
-- TRUNCATE TABLE `outside_cost_record`;

-- ============ CRM 模块（如果使用）============
-- 如果有 CRM 模块的测试数据，可以执行单独的清理脚本
-- SOURCE sql/mysql/cleanup_crm_test_data.sql;

-- =====================================================
-- 第八部分：清理测试用户数据，只保留超级管理员
-- =====================================================

-- 删除非管理员用户（保留 id=1 的超级管理员）
DELETE FROM `system_users` WHERE `id` > 1 AND `tenant_id` = 1;

-- 删除非管理员的用户角色关联
DELETE FROM `system_user_role` WHERE `user_id` > 1;

-- 删除非管理员的用户岗位关联
DELETE FROM `system_user_post` WHERE `user_id` > 1;

-- =====================================================
-- 第九部分：清理非默认租户数据（如果只保留默认租户）
-- =====================================================

-- 如果是单租户模式，可以删除其他租户
-- DELETE FROM `system_tenant` WHERE `id` > 1;
-- DELETE FROM `system_users` WHERE `tenant_id` > 1;
-- DELETE FROM `system_dept` WHERE `tenant_id` > 1;
-- DELETE FROM `system_role` WHERE `tenant_id` > 1;
-- DELETE FROM `system_role_menu` WHERE `role_id` NOT IN (SELECT id FROM system_role WHERE tenant_id = 1);

-- =====================================================
-- 第十部分：重置管理员密码（密码: admin123）
-- 注意：生产环境请修改为强密码！
-- =====================================================

-- 密码 admin123 的 BCrypt 加密值
UPDATE `system_users` 
SET `password` = '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm',
    `mobile` = '',
    `avatar` = ''
WHERE `id` = 1;

-- =====================================================
-- 第十一部分：清理文件存储配置中的测试密钥
-- =====================================================

-- 删除测试用的文件存储配置（保留数据库存储作为默认）
DELETE FROM `infra_file_config` WHERE `id` > 4;

-- 更新数据库存储为主配置
UPDATE `infra_file_config` SET `master` = b'1' WHERE `id` = 4;
UPDATE `infra_file_config` SET `master` = b'0' WHERE `id` != 4;

-- =====================================================
-- 第十二部分：清理数据源配置
-- =====================================================

TRUNCATE TABLE `infra_data_source_config`;

-- =====================================================
-- 第十三部分：重置自增 ID（可选）
-- =====================================================

-- 如果需要重置表的自增 ID，可以取消以下注释
-- ALTER TABLE `infra_api_access_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `infra_api_error_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `infra_job_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `system_login_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `system_operate_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `system_mail_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `system_sms_log` AUTO_INCREMENT = 1;
-- ALTER TABLE `system_notify_message` AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 初始化完成提示
-- =====================================================

SELECT '========================================' AS '';
SELECT '生产环境数据库初始化完成！' AS '提示';
SELECT '========================================' AS '';
SELECT '默认管理员账号: admin' AS '账号';
SELECT '默认管理员密码: admin123' AS '密码';
SELECT '请立即修改管理员密码！' AS '警告';
SELECT '========================================' AS '';
