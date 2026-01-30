-- 删除服务项的金额字段
-- 金额分配功能将迁移到成本管理模块的合同收入分配功能中

-- 删除 amount 列
ALTER TABLE project_info DROP COLUMN amount;
