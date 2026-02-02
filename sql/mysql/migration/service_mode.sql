-- ==============================================
-- 服务项服务模式 - 数据库迁移脚本
-- 
-- 功能：给服务项表添加 service_mode 字段，区分驻场和二线服务
-- 
-- service_mode:
--   1 = 驻场服务（需要驻场人员在客户现场）
--   2 = 二线服务（远程/定期服务）
-- 
-- 规则：
--   - 安全运营（deptType=2）默认为驻场（service_mode=1）
--   - 安全服务（deptType=1）可选驻场或二线
--   - 数据安全（deptType=3）默认为二线（service_mode=2）
-- ==============================================

-- 1. 添加 service_mode 字段
ALTER TABLE `project_info` ADD COLUMN `service_mode` TINYINT DEFAULT 2 
    COMMENT '服务模式：1-驻场 2-二线' AFTER `dept_type`;

-- 2. 更新现有数据
-- 安全运营的服务项设为驻场
UPDATE `project_info` SET `service_mode` = 1 WHERE `dept_type` = 2 AND `deleted` = 0;

-- 安全服务和数据安全默认为二线（已经是默认值2，无需更新）

-- 3. 添加索引（可选，提升查询效率）
ALTER TABLE `project_info` ADD INDEX `idx_service_mode` (`service_mode`);
ALTER TABLE `project_info` ADD INDEX `idx_project_service_mode` (`project_id`, `service_mode`);

-- 4. 验证
-- SELECT dept_type, service_mode, COUNT(*) FROM project_info WHERE deleted = 0 GROUP BY dept_type, service_mode;
