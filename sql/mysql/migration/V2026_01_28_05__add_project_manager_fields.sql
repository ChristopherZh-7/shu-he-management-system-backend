-- 为项目表添加负责人字段（支持多选）
-- V2026_01_28_05

-- 添加负责人ID列表字段（JSON数组）
ALTER TABLE `project` ADD COLUMN `manager_ids` JSON DEFAULT NULL COMMENT '负责人ID列表（JSON数组）' AFTER `description`;

-- 添加负责人名称列表字段（JSON数组）
ALTER TABLE `project` ADD COLUMN `manager_names` JSON DEFAULT NULL COMMENT '负责人名称列表（JSON数组）' AFTER `manager_ids`;
