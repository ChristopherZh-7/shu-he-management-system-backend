-- =====================================================
-- 同步已部署环境的钉钉通知模板：${serviceItemName} → ${serviceTypeName}
--
-- 背景：V2026_02_05_04 在 system_dingtalk_notification_config 表里 INSERT 了一条
--   event_type='round_deadline_remind' 的配置，content_template 使用了 ${serviceItemName}
--   占位符。本次需求把「服务项名称」字段全部清理后，触发方（RoundDeadlineRemindJob）
--   已改为发送 ${serviceTypeName}；存量配置必须同步 UPDATE，否则模板渲染会出现 null。
--
-- 兼容性：
--   - 新环境：V2026_02_05_04 已被本次改为直接 INSERT ${serviceTypeName}，本 patch 无副作用
--     （REPLACE 不到匹配子串，UPDATE 影响 0 行）。
--   - 老环境：把库里旧字段名一次性改为新字段名，并把行内文案「服务项名称：」改为「服务类型：」
--     以便用户在通知里看到一致的字段语义。
--
-- 回滚：
--   UPDATE `system_dingtalk_notification_config`
--   SET `content_template` = REPLACE(REPLACE(`content_template`,
--           '${serviceTypeName}', '${serviceItemName}'),
--           '服务类型：', '服务项名称：')
--   WHERE `event_type` = 'round_deadline_remind' AND `deleted` = 0;
-- =====================================================

UPDATE `system_dingtalk_notification_config`
SET `content_template` = REPLACE(REPLACE(`content_template`,
        '${serviceItemName}', '${serviceTypeName}'),
        '服务项名称：', '服务类型：')
WHERE `event_type` = 'round_deadline_remind'
  AND `deleted` = 0;
