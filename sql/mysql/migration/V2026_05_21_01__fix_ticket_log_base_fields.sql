-- =====================================================
-- 修复 shuhe_ticket_log 缺少 BaseDO 基础字段
-- =====================================================
-- TicketLogDO 继承 BaseDO，MyBatis-Plus 插入时会自动填充
-- update_time / updater，并且查询时会带 deleted 逻辑删除条件。
-- 旧版 V2026_05_19_01 建表脚本漏了这些字段，导致创建工单写日志时报：
-- Unknown column 'update_time' in 'field list'。

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shuhe_ticket_log'
      AND COLUMN_NAME = 'update_time'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `shuhe_ticket_log` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `create_time`',
    'SELECT ''shuhe_ticket_log.update_time already exists, skip'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shuhe_ticket_log'
      AND COLUMN_NAME = 'updater'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `shuhe_ticket_log` ADD COLUMN `updater` VARCHAR(64) DEFAULT '''' AFTER `creator`',
    'SELECT ''shuhe_ticket_log.updater already exists, skip'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shuhe_ticket_log'
      AND COLUMN_NAME = 'deleted'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `shuhe_ticket_log` ADD COLUMN `deleted` BIT(1) NOT NULL DEFAULT b''0'' AFTER `updater`',
    'SELECT ''shuhe_ticket_log.deleted already exists, skip'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
