-- 简化 CRM 服务项管理：编码、单位、价格、分类改为可空
-- 服务项仅保留名称、状态、描述、负责人，价格在创建项目服务项时从合同部门预算分配

ALTER TABLE `crm_product`
  MODIFY COLUMN `no` VARCHAR(50) NULL COMMENT '产品编码（已废弃，可空）',
  MODIFY COLUMN `unit` INT NULL COMMENT '单位（已废弃，可空）',
  MODIFY COLUMN `price` DECIMAL(15,2) NULL COMMENT '价格（已废弃，可空）',
  MODIFY COLUMN `category_id` BIGINT NULL COMMENT '产品分类（已废弃，可空）';
