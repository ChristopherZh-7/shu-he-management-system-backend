-- =====================================================
-- Migration: V2026_06_12_01__fix_round_remind_template_variable.sql
-- Date: 2026-06-12
-- Description: 修正「执行计划截止提醒」钉钉通知模板的占位符
--   背景：V2026_02_05_04 旧版 INSERT 的模板用 ${serviceItemName}，
--         后端 RoundDeadlineRemindJob 实际下发变量为 serviceTypeName，
--         导致已应用旧版迁移的环境占位符无法替换。
--         （迁移文件后来虽已改为 ${serviceTypeName}，但按文件名跟踪的
--         环境不会重跑旧迁移，故补此 UPDATE。）
--   幂等：仅命中仍包含 ${serviceItemName} 的记录，可重复执行。
-- =====================================================

UPDATE `system_dingtalk_notification_config`
SET `content_template` = REPLACE(
        REPLACE(`content_template`, '${serviceItemName}', '${serviceTypeName}'),
        '服务项名称：', '服务类型：'),
    `update_time` = NOW()
WHERE `event_type` = 'round_deadline_remind'
  AND `content_template` LIKE '%${serviceItemName}%';
