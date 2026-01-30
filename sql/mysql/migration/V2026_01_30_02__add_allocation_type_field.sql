-- 为 service_item_allocation 表添加 allocation_type 字段
-- 用于区分普通服务项分配和安全运营费用分配

ALTER TABLE `service_item_allocation` 
ADD COLUMN `allocation_type` VARCHAR(32) DEFAULT 'service_item' COMMENT '分配类型：service_item-服务项分配, so_management-安全运营管理费, so_onsite-安全运营驻场费' AFTER `contract_dept_allocation_id`;

-- 修改 service_item_id 字段为可空（安全运营分配不需要关联服务项）
ALTER TABLE `service_item_allocation` 
MODIFY COLUMN `service_item_id` BIGINT NULL COMMENT '服务项ID（服务项分配时必填，安全运营分配时为空）';

-- 更新现有数据的分配类型为默认值（服务项分配）
UPDATE `service_item_allocation` SET `allocation_type` = 'service_item' WHERE `allocation_type` IS NULL;

-- 添加索引，方便按类型查询
CREATE INDEX `idx_allocation_type` ON `service_item_allocation` (`allocation_type`);
