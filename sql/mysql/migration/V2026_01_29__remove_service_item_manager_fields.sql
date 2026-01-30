-- 删除服务项表（project_info）的负责人字段
-- 服务项不再需要单独的负责人，项目层面已有项目负责人

ALTER TABLE `project_info` DROP COLUMN `manager_id`;
ALTER TABLE `project_info` DROP COLUMN `manager_name`;
