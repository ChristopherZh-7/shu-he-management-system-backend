-- =============================================
-- 修复「CRM 待办事项」页面「没有该操作权限」问题
--
-- 原因：待办事项页面加载时会调用 8 个接口，需要以下权限：
--   - crm:customer:query   (今日需联系、分配客户、待进入公海)
--   - crm:clue:query       (分配线索)
--   - crm:contract:query   (待审核合同、即将到期合同)
--   - crm:receivable:query (待审核回款)
--   - crm:receivable-plan:query (待回款提醒)
--
-- 若角色只有「待办事项」菜单但缺少上述按钮权限，会 403。
--
-- 本脚本：为所有拥有「待办事项」菜单的角色，补充上述 5 个查询权限。
-- =============================================

SET NAMES utf8mb4;

-- 待办事项菜单 ID
SET @backlog_menu_id = (SELECT id FROM system_menu WHERE path = 'backlog' AND parent_id IN (SELECT id FROM system_menu WHERE path = '/crm' AND deleted = 0) AND deleted = 0 LIMIT 1);

-- 5 个必需的权限菜单 ID
SET @customer_query_id = (SELECT id FROM system_menu WHERE permission = 'crm:customer:query' AND deleted = 0 LIMIT 1);
SET @clue_query_id = (SELECT id FROM system_menu WHERE permission = 'crm:clue:query' AND deleted = 0 LIMIT 1);
SET @contract_query_id = (SELECT id FROM system_menu WHERE permission = 'crm:contract:query' AND deleted = 0 LIMIT 1);
SET @receivable_query_id = (SELECT id FROM system_menu WHERE permission = 'crm:receivable:query' AND deleted = 0 LIMIT 1);
SET @receivable_plan_query_id = (SELECT id FROM system_menu WHERE permission = 'crm:receivable-plan:query' AND deleted = 0 LIMIT 1);

-- 为拥有「待办事项」菜单的角色，补充 5 个查询权限（若尚未拥有）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @customer_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @backlog_menu_id AND rm.deleted = 0
  AND @customer_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @customer_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @clue_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @backlog_menu_id AND rm.deleted = 0
  AND @clue_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @clue_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @contract_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @backlog_menu_id AND rm.deleted = 0
  AND @contract_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @contract_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @receivable_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @backlog_menu_id AND rm.deleted = 0
  AND @receivable_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @receivable_query_id AND rm2.deleted = 0);

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @receivable_plan_query_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @backlog_menu_id AND rm.deleted = 0
  AND @receivable_plan_query_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @receivable_plan_query_id AND rm2.deleted = 0);

SELECT 'fix_crm_backlog_permissions done. Re-login to take effect.' AS result;
