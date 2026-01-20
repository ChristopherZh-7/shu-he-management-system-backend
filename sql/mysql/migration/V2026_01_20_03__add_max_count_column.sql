-- 补充添加 max_count 字段
-- 因为之前的迁移已执行过部分内容

-- 检查并添加 max_count 字段（如果不存在）
SET @dbname = DATABASE();
SET @tablename = 'project_info';
SET @columnname = 'max_count';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `project_info` ADD COLUMN `max_count` int DEFAULT 1 COMMENT ''合同期内最大执行次数（按需时无效）'' AFTER `frequency_type`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
