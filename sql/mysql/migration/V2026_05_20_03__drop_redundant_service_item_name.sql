-- =====================================================
-- 删除 3 张表中的冗余 service_item_name 列
--
-- 背景：业务上「服务项名称」已统一被「服务类型」取代（V2026_05_20_02 已
--   drop project_info.name）。下游 3 张表的 service_item_name 冗余列同步清理：
--     1) outside_cost_record           -- 外出成本记录
--     2) project_management_record     -- 项目管理记录（原 project_work_record）
--     3) service_item_allocation       -- 服务项分配
--
-- 替代字段：3 张表均已有 service_type 列（字典值），前端展示用字典中文 label。
--
-- 备份建议（在跑此 migration 前自行执行）：
--   mysqldump -uroot ruoyi-vue-pro \
--     outside_cost_record project_management_record service_item_allocation \
--     > backup_service_item_name_$(date +%Y%m%d_%H%M%S).sql
--
--   或仅备份这 3 列：
--   CREATE TABLE _backup_outside_cost_record_sname AS
--     SELECT id, service_item_name FROM outside_cost_record WHERE service_item_name IS NOT NULL;
--   CREATE TABLE _backup_project_mgmt_record_sname AS
--     SELECT id, service_item_name FROM project_management_record WHERE service_item_name IS NOT NULL;
--   CREATE TABLE _backup_service_item_alloc_sname AS
--     SELECT id, service_item_name FROM service_item_allocation WHERE service_item_name IS NOT NULL;
--
-- 回滚：
--   ALTER TABLE `outside_cost_record`       ADD COLUMN `service_item_name` varchar(255) DEFAULT NULL COMMENT '服务项名称（快照）' AFTER `service_item_id`;
--   ALTER TABLE `project_management_record` ADD COLUMN `service_item_name` varchar(255) DEFAULT NULL COMMENT '服务项名称(冗余)'   AFTER `service_item_id`;
--   ALTER TABLE `service_item_allocation`   ADD COLUMN `service_item_name` varchar(128) DEFAULT NULL COMMENT '服务项名称（冗余）' AFTER `service_item_id`;
--   -- 若做过 _backup_* 备份表，可 JOIN 回填。
-- =====================================================

ALTER TABLE `outside_cost_record`       DROP COLUMN `service_item_name`;
ALTER TABLE `project_management_record` DROP COLUMN `service_item_name`;
ALTER TABLE `service_item_allocation`   DROP COLUMN `service_item_name`;
