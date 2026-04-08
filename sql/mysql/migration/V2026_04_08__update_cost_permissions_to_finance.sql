-- ============================================================
-- 成本管理 → 财务管理 模块合并迁移
-- ============================================================

-- 1. 更新权限键：system:xxx → finance:xxx
UPDATE system_menu
SET permission = REPLACE(permission, 'system:cost:', 'finance:cost:'),
    updater = '1', update_time = NOW()
WHERE permission LIKE 'system:cost:%' AND deleted = 0;

UPDATE system_menu
SET permission = REPLACE(permission, 'system:business-analysis:', 'finance:business-analysis:'),
    updater = '1', update_time = NOW()
WHERE permission LIKE 'system:business-analysis:%' AND deleted = 0;

UPDATE system_menu
SET permission = REPLACE(permission, 'system:outside-cost:', 'finance:outside-cost:'),
    updater = '1', update_time = NOW()
WHERE permission LIKE 'system:outside-cost:%' AND deleted = 0;

UPDATE system_menu
SET permission = REPLACE(permission, 'system:dept-cost-summary:', 'finance:dept-cost-summary:'),
    updater = '1', update_time = NOW()
WHERE permission LIKE 'system:dept-cost-summary:%' AND deleted = 0;

-- 2. 将成本管理子菜单移到财务管理下（parent_id 5116 → 5300）
-- 经营分析
UPDATE system_menu SET parent_id = 5300, component = 'finance/business-analysis/index', updater = '1', update_time = NOW()
WHERE id = 5187 AND deleted = 0;

-- 员工成本列表
UPDATE system_menu SET parent_id = 5300, component = 'finance/user-cost/index', updater = '1', update_time = NOW()
WHERE id = 5123 AND deleted = 0;

-- 跨部门项目费用
UPDATE system_menu SET parent_id = 5300, component = 'finance/outside-cost/index', updater = '1', update_time = NOW()
WHERE id = 5152 AND deleted = 0;

-- 成本查询（权限按钮）移到财务管理下
UPDATE system_menu SET parent_id = 5300, updater = '1', update_time = NOW()
WHERE id = 5117 AND deleted = 0;

-- 3. 将岗位历史移到系统管理下
SET @system_menu_id = (SELECT id FROM system_menu WHERE name = '系统管理' AND parent_id = 0 AND deleted = 0 LIMIT 1);

UPDATE system_menu SET parent_id = @system_menu_id, updater = '1', update_time = NOW()
WHERE id = 5118 AND deleted = 0;

-- 4. 子权限菜单也要跟着移（经营分析的子权限）
-- 经营分析查询权限（parent_id=5187，已经跟着5187移了）

-- 5. 软删除旧的成本管理一级菜单
UPDATE system_menu SET deleted = 1, updater = '1', update_time = NOW()
WHERE id = 5116;
