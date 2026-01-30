-- =============================================
-- 部门服务单状态字段迁移
-- 旧状态: 0-待领取 1-进行中 2-已暂停 3-已完成 4-已取消
-- 新状态: 0-待领取 1-待开始 2-进行中 3-已暂停 4-已完成 5-已取消
-- =============================================

-- 更新状态值（从大到小，避免覆盖）
-- 原4(已取消) -> 新5(已取消)
UPDATE project_dept_service SET status = 5 WHERE status = 4 AND deleted = 0;

-- 原3(已完成) -> 新4(已完成)
UPDATE project_dept_service SET status = 4 WHERE status = 3 AND deleted = 0;

-- 原2(已暂停) -> 新3(已暂停)
UPDATE project_dept_service SET status = 3 WHERE status = 2 AND deleted = 0;

-- 原1(进行中) -> 新2(进行中)
UPDATE project_dept_service SET status = 2 WHERE status = 1 AND deleted = 0;

-- 注意：原0(待领取)保持不变
-- 注意：如果有已领取但尚未开始的记录（claimed=1 且 status=0），需要迁移到新的待开始状态(1)
UPDATE project_dept_service SET status = 1 WHERE claimed = 1 AND status = 0 AND deleted = 0;

-- 添加迁移日志
SELECT CONCAT('迁移完成，待领取:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 0 AND deleted = 0),
    ', 待开始:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 1 AND deleted = 0),
    ', 进行中:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 2 AND deleted = 0),
    ', 已暂停:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 3 AND deleted = 0),
    ', 已完成:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 4 AND deleted = 0),
    ', 已取消:', 
    (SELECT COUNT(*) FROM project_dept_service WHERE status = 5 AND deleted = 0)
) AS migration_result;
