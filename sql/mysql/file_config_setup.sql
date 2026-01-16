-- =============================================
-- 文件存储配置 - 解决图片上传问题
-- 执行时间: 2026-01-15
-- =============================================

-- 1. 先将所有现有配置设为非主配置
UPDATE `infra_file_config` SET `master` = b'0' WHERE `deleted` = 0;

-- 2. 检查是否已存在数据库存储配置
SET @db_config_exists = (SELECT COUNT(*) FROM `infra_file_config` WHERE `storage` = 20 AND `deleted` = 0);

-- 3. 如果不存在，则创建数据库存储配置
-- 数据库存储（storage=20）不需要配置外部路径，最简单
INSERT INTO `infra_file_config` (`name`, `storage`, `remark`, `master`, `config`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) 
SELECT '数据库存储', 20, '适合小文件存储，图片会存入数据库', b'1', 
       '{"@class":"cn.shuhe.system.module.infra.framework.file.core.client.db.DBFileClientConfig","domain":"http://127.0.0.1:48080"}', 
       '1', NOW(), '1', NOW(), b'0'
WHERE @db_config_exists = 0;

-- 4. 如果已存在数据库存储配置，将其设为主配置
UPDATE `infra_file_config` SET `master` = b'1' WHERE `storage` = 20 AND `deleted` = 0 LIMIT 1;

-- 5. 确保至少有一个主配置
-- 如果数据库存储也没有成功设置，则将第一个可用配置设为主配置
SET @has_master = (SELECT COUNT(*) FROM `infra_file_config` WHERE `master` = 1 AND `deleted` = 0);
UPDATE `infra_file_config` SET `master` = b'1' WHERE `deleted` = 0 AND @has_master = 0 LIMIT 1;

-- 查看当前配置
SELECT id, name, storage, 
       CASE storage 
           WHEN 10 THEN '本地存储' 
           WHEN 11 THEN 'FTP存储' 
           WHEN 12 THEN 'SFTP存储' 
           WHEN 20 THEN '数据库存储' 
           WHEN 21 THEN 'S3存储' 
           ELSE '其他' 
       END as storage_type,
       IF(master=1, '是', '否') as is_master,
       remark
FROM `infra_file_config` 
WHERE `deleted` = 0;
