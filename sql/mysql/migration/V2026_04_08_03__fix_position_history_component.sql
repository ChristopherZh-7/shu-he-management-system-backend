-- 修复岗位历史页面的 component 路径（从旧的 cost-management 改为 finance）
UPDATE system_menu SET component = 'finance/position-history/index', updater = '1', update_time = NOW()
WHERE id = 5118 AND deleted = 0;
