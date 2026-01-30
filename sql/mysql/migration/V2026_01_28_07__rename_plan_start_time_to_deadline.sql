-- 将 project_round 表的 plan_start_time 字段重命名为 deadline（截止日期）
-- 业务含义：从"计划开始时间"改为"任务截止日期"

ALTER TABLE `project_round` 
    CHANGE COLUMN `plan_start_time` `deadline` datetime DEFAULT NULL COMMENT '截止日期';

-- 更新字段注释
ALTER TABLE `project_round` 
    MODIFY COLUMN `deadline` datetime DEFAULT NULL COMMENT '截止日期（任务应在此日期前完成）';
