-- =============================================
-- 修复「商机转合同」签合同时「没有该操作权限」问题
--
-- 原因：商机详情页点击「签合同」调用 POST /crm/contract/create，
--       需要 crm:contract:create 权限；提交审批需要 crm:contract:update。
--       若角色有商机菜单但缺少合同创建/更新权限，会 403。
--
-- 本脚本：为所有拥有「商机管理」菜单的角色，补充 crm:contract:create 和 crm:contract:update。
-- =============================================

SET NAMES utf8mb4;

-- 商机管理菜单 ID（path='business' 且 parent 为 CRM）
SET @business_menu_id = (SELECT id FROM system_menu WHERE path = 'business' AND parent_id IN (SELECT id FROM system_menu WHERE path = '/crm' AND deleted = 0) AND deleted = 0 LIMIT 1);

-- 合同创建、合同更新菜单 ID
SET @contract_create_id = (SELECT id FROM system_menu WHERE permission = 'crm:contract:create' AND deleted = 0 LIMIT 1);
SET @contract_update_id = (SELECT id FROM system_menu WHERE permission = 'crm:contract:update' AND deleted = 0 LIMIT 1);

-- 为拥有「商机管理」菜单的角色，补充 crm:contract:create（若尚未拥有）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @contract_create_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @business_menu_id AND rm.deleted = 0
  AND @contract_create_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @contract_create_id AND rm2.deleted = 0);

-- 为拥有「商机管理」菜单的角色，补充 crm:contract:update（若尚未拥有）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT rm.role_id, @contract_update_id, '1', NOW(), '1', NOW(), b'0'
FROM system_role_menu rm
WHERE rm.menu_id = @business_menu_id AND rm.deleted = 0
  AND @contract_update_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = @contract_update_id AND rm2.deleted = 0);

SELECT 'fix_crm_contract_create_permissions done. 请重新登录使权限生效.' AS result;
