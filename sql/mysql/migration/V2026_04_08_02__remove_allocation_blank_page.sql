-- 移除"收入分配管理"空白页菜单（功能已合并到"项目预算管理"页面中）
UPDATE system_menu SET deleted = 1, updater = '1', update_time = NOW()
WHERE id IN (5310, 5311, 5312, 5313, 5314);
