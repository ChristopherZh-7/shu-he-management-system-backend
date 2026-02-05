-- 数据安全模块采用安全服务模式（驻场/二线）
-- 改造说明：
-- 1. 数据安全(deptType=3)现在使用 serviceMode 字段区分驻场(1)和二线(2)服务项
-- 2. 数据安全部门服务单可以设置驻场负责人和二线负责人（复用现有字段）
-- 3. 数据安全驻场服务项不能通过服务发起功能发起（需通过驻场点管理）
-- 4. 费用分配新增 ds_onsite（数据安全驻场费）和 ds_second_line（数据安全二线费）两种类型

-- 现有字段复用说明：
-- project_info.service_mode: 1-驻场, 2-二线（安全服务和数据安全共用）
-- project_dept_service.onsite_manager_ids: 驻场负责人ID列表（安全服务和数据安全共用）
-- project_dept_service.onsite_manager_names: 驻场负责人姓名列表
-- project_dept_service.second_line_manager_ids: 二线负责人ID列表
-- project_dept_service.second_line_manager_names: 二线负责人姓名列表

-- 无需新增字段，仅更新字段注释以反映新用途

-- 更新 project_info 表的 service_mode 字段注释
ALTER TABLE `project_info` MODIFY COLUMN `service_mode` TINYINT DEFAULT NULL 
COMMENT '服务模式（安全服务和数据安全使用）：1-驻场 2-二线';

-- 更新 project_dept_service 表的相关字段注释
ALTER TABLE `project_dept_service` MODIFY COLUMN `onsite_manager_ids` JSON DEFAULT NULL 
COMMENT '驻场负责人ID列表（JSON数组）- 安全服务(deptType=1)和数据安全(deptType=3)使用';

ALTER TABLE `project_dept_service` MODIFY COLUMN `onsite_manager_names` JSON DEFAULT NULL 
COMMENT '驻场负责人姓名列表（JSON数组）- 安全服务(deptType=1)和数据安全(deptType=3)使用';

ALTER TABLE `project_dept_service` MODIFY COLUMN `second_line_manager_ids` JSON DEFAULT NULL 
COMMENT '二线负责人ID列表（JSON数组）- 安全服务(deptType=1)和数据安全(deptType=3)使用';

ALTER TABLE `project_dept_service` MODIFY COLUMN `second_line_manager_names` JSON DEFAULT NULL 
COMMENT '二线负责人姓名列表（JSON数组）- 安全服务(deptType=1)和数据安全(deptType=3)使用';

-- 更新 service_item_allocation 表的 allocation_type 字段注释
ALTER TABLE `service_item_allocation` MODIFY COLUMN `allocation_type` VARCHAR(32) DEFAULT 'service_item' 
COMMENT '分配类型：service_item-服务项分配, so_management-安全运营管理费, so_onsite-安全运营驻场费, ss_onsite-安全服务驻场费, ss_second_line-安全服务二线费, ds_onsite-数据安全驻场费, ds_second_line-数据安全二线费';
