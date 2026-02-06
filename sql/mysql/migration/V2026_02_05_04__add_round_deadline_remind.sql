-- ========================================
-- 执行计划截止日期提醒功能
-- 功能说明：
-- 1. 为执行计划(Round)添加提醒相关字段
-- 2. 添加默认的通知配置（需要手动启用）
-- ========================================

-- 1. 为 project_round 表添加提醒相关字段
ALTER TABLE `project_round` 
ADD COLUMN `remind_days` INT DEFAULT 3 COMMENT '提前几天提醒' AFTER `deadline`,
ADD COLUMN `remind_time` DATETIME DEFAULT NULL COMMENT '提醒时间（自动计算：deadline - remind_days）' AFTER `remind_days`,
ADD COLUMN `reminded` TINYINT(1) DEFAULT 0 COMMENT '是否已提醒：0-未提醒 1-已提醒' AFTER `remind_time`;

-- 2. 为已有数据计算 remind_time（提前3天）
UPDATE `project_round` 
SET `remind_time` = DATE_SUB(`deadline`, INTERVAL 3 DAY)
WHERE `deadline` IS NOT NULL AND `remind_time` IS NULL;

-- 3. 添加索引以加速定时任务查询
CREATE INDEX `idx_round_remind` ON `project_round` (`remind_time`, `reminded`, `status`);

-- 4. 添加默认的执行计划截止提醒通知配置（默认停用，需要管理员手动启用并配置机器人）
-- 注意：robot_id 需要根据实际情况修改
INSERT INTO `system_dingtalk_notification_config` (
    `name`, 
    `robot_id`,
    `event_type`, 
    `event_module`, 
    `msg_type`,
    `title_template`, 
    `content_template`,
    `at_type`,
    `status`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`
) VALUES (
    '执行计划截止提醒',
    1, -- 需要根据实际机器人ID修改
    'round_deadline_remind',
    'project',
    'markdown',
    '⏰ 服务执行计划即将到期',
    '### ⏰ 服务执行计划即将到期

**服务项名称：** ${serviceItemName}

**执行计划：** ${roundName}

**截止日期：** ${deadline}

**剩余天数：** ${remainingDays} 天

**客户名称：** ${customerName}

---
请及时完成相关工作！',
    0, -- 不@任何人（会单独处理@逻辑）
    1, -- 默认停用，需要手动启用
    '执行计划截止日期提前提醒，定时任务自动触发',
    '1',
    NOW(),
    '1',
    NOW(),
    0
);

-- 5. 添加定时任务配置（仅供参考，需要在系统中手动配置）
-- 建议 CRON 表达式：0 0 9 * * ? （每天早上9点执行）
-- 任务处理器名称：roundDeadlineRemindJob
