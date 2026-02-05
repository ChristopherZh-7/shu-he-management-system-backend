-- 安全运营服务项归属人员类型字段
-- 用于区分服务项是由管理人员负责还是驻场人员负责
-- 仅对安全运营(deptType=2)生效，安全服务(deptType=1)使用 serviceMode 区分

-- 1. 添加 service_member_type 字段
ALTER TABLE `project_info` ADD COLUMN `service_member_type` TINYINT DEFAULT NULL 
COMMENT '服务项归属人员类型（安全运营专用）：1-驻场人员 2-管理人员';

-- 2. 添加索引
ALTER TABLE `project_info` ADD INDEX `idx_service_member_type` (`service_member_type`);

-- 3. 为现有的安全运营服务项设置默认值（管理人员，以便可以被服务发起）
-- 注意：新建驻场服务项时需要手动设置为 1
UPDATE `project_info` SET `service_member_type` = 2 WHERE `dept_type` = 2 AND `deleted` = 0;
