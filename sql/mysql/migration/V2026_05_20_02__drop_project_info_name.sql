-- =====================================================
-- 删除服务项（project_info）的 name 字段
--
-- 背景：业务上「服务项名称」已被「服务类型字典中文 label + 编号后4位」取代
--   - 列表/详情/工单/轮次等所有地方不再依赖 project_info.name
--   - 应用层统一用 displayName 计算字段（ServiceItemService.getDisplayName）
--
-- 备份建议（在跑此 migration 前自行执行）：
--   mysqldump -uroot ruoyi-vue-pro project_info > backup_project_info_$(date +%Y%m%d_%H%M%S).sql
--   或仅备份 name 列：
--   CREATE TABLE _backup_project_info_name AS
--   SELECT id, name FROM project_info WHERE name IS NOT NULL;
--
-- 回滚：
--   ALTER TABLE project_info ADD COLUMN name varchar(200) DEFAULT NULL COMMENT '服务项名称';
--   -- 若执行前做过 _backup_project_info_name，则可：
--   -- UPDATE project_info pi JOIN _backup_project_info_name b ON pi.id = b.id SET pi.name = b.name;
-- =====================================================

ALTER TABLE `project_info` DROP COLUMN `name`;
