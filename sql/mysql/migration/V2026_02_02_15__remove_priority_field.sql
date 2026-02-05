-- 删除服务项表的优先级字段
-- 该字段已不再使用，从前后端代码中移除

ALTER TABLE project_info DROP COLUMN priority;
