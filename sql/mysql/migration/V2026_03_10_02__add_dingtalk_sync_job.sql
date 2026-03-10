-- =============================================
-- 钉钉用户同步计划任务
-- 每周一凌晨 2 点执行，同步所有启用的钉钉配置的用户数据
-- 可在 基础设施 -> 定时任务 中查看和修改执行时间
-- =============================================

INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`, `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES ('钉钉用户同步 Job', 1, 'dingtalkSyncJob', NULL, '0 0 2 ? * MON', 3, 5000, 0, '1', NOW(), '1', NOW(), 0);
