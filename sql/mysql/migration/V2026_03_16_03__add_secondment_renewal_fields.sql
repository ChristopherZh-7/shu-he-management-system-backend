-- =====================================================
-- 借调申请 - 续借与到期标记
-- 1. 续借关联字段
-- 2. 表注释更新为借调申请
-- 日期: 2026-03-16
-- =====================================================

-- 1. 添加续借关联字段（续借时关联原借调记录ID）
ALTER TABLE `project_service_launch`
    ADD COLUMN `renewal_of_id` bigint DEFAULT NULL COMMENT '续借自哪条借调记录ID（续借时填充）' AFTER `round_id`;

-- 2. 添加索引便于查询续借链
ALTER TABLE `project_service_launch`
    ADD INDEX `idx_renewal_of_id` (`renewal_of_id`);

-- 3. 更新表注释
ALTER TABLE `project_service_launch` COMMENT = '借调申请表（原统一服务发起）';
