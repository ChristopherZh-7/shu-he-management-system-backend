-- =====================================================
-- 借调发起入口收敛到工单中心（服务派遣）
--   - 软删「借调申请」菜单（原 工作流程/服务项发起/借调申请）
--   - 「服务项发起」目录下无其他可见子菜单时连目录一并软删
--   - 借调底层引擎（project_service_launch 表 / 轮次 / 外出确认）保留不动
--   - 借调详情页保留（前端 hideInMenu 路由，从工单详情跳转）
-- =====================================================

SET @service_initiate = (SELECT `id` FROM `system_menu`
                         WHERE `name` = '服务项发起' AND `deleted` = b'0' LIMIT 1);

-- 1. 软删「借调申请」菜单入口
UPDATE `system_menu`
SET `deleted` = b'1', `updater` = '1', `update_time` = NOW()
WHERE `name` = '借调申请'
  AND `type` = 2
  AND `deleted` = b'0';

-- 2. 「服务项发起」目录下已无可见子菜单时，连目录一并软删
UPDATE `system_menu`
SET `deleted` = b'1', `updater` = '1', `update_time` = NOW()
WHERE `id` = @service_initiate
  AND @service_initiate IS NOT NULL
  AND NOT EXISTS (
        SELECT 1 FROM (
            SELECT `id` FROM `system_menu`
            WHERE `parent_id` = @service_initiate AND `deleted` = b'0'
        ) t
  );
