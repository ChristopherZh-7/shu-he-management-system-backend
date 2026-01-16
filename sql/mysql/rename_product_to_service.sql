-- =============================================
-- 将 CRM "产品管理" 重命名为 "服务项管理"
-- 执行时间: 2026-01-15
-- =============================================

-- 1. 修改菜单名称
UPDATE system_menu SET name = '服务项管理' WHERE id = 2526 AND name = '产品管理';
UPDATE system_menu SET name = '服务项查询' WHERE id = 2527 AND name = '产品查询';
UPDATE system_menu SET name = '服务项创建' WHERE id = 2528 AND name = '产品创建';
UPDATE system_menu SET name = '服务项更新' WHERE id = 2529 AND name = '产品更新';
UPDATE system_menu SET name = '服务项删除' WHERE id = 2530 AND name = '产品删除';
UPDATE system_menu SET name = '服务项导出' WHERE id = 2531 AND name = '产品导出';
UPDATE system_menu SET name = '服务项分类配置' WHERE id = 2532 AND name = '产品分类配置';
UPDATE system_menu SET name = '服务项分类查询' WHERE id = 2533 AND name = '产品分类查询';
UPDATE system_menu SET name = '服务项分类创建' WHERE id = 2534 AND name = '产品分类创建';
UPDATE system_menu SET name = '服务项分类更新' WHERE id = 2535 AND name = '产品分类更新';
UPDATE system_menu SET name = '服务项分类删除' WHERE id = 2536 AND name = '产品分类删除';

-- 2. 修改字典类型名称
UPDATE system_dict_type SET name = '服务项状态' WHERE type = 'crm_product_status';
UPDATE system_dict_type SET name = 'CRM 服务项单位' WHERE type = 'crm_product_unit';

-- 3. 验证修改结果
SELECT id, name FROM system_menu WHERE id IN (2526, 2527, 2528, 2529, 2530, 2531, 2532, 2533, 2534, 2535, 2536);
SELECT id, name, type FROM system_dict_type WHERE type IN ('crm_product_status', 'crm_product_unit');

SELECT '✅ CRM 产品管理已重命名为服务项管理！' AS result;
