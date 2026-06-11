-- =====================================================
-- 工单中心菜单精简
--   - 「我的工单」并入「工单列表」（页面内 指派给我/我提交的/全部 三视图切换）
--   - 「工单分类」暂不使用，下线菜单（后端分类接口与表保留，未来可恢复）
--   - 「工单详情」隐藏路由菜单保留不动
-- =====================================================

UPDATE `system_menu`
SET `deleted` = b'1', `updater` = '1', `update_time` = NOW()
WHERE `component` = 'ticket/my/index'
  AND `deleted` = b'0';

UPDATE `system_menu`
SET `deleted` = b'1', `updater` = '1', `update_time` = NOW()
WHERE `component` = 'ticket/category/index'
  AND `deleted` = b'0';
