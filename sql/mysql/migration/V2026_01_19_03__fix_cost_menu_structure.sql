-- 修复成本管理菜单结构
-- 将"成本管理"从页面菜单改为目录菜单，并添加"员工成本列表"子菜单

-- 1. 将成本管理菜单改为目录类型（type=1），清空component
UPDATE system_menu 
SET type = 1, 
    component = '', 
    component_name = '',
    always_show = 1
WHERE name = '成本管理' AND deleted = 0;

-- 2. 获取成本管理菜单ID
SET @cost_menu_id = (SELECT id FROM system_menu WHERE name = '成本管理' AND deleted = 0 LIMIT 1);

-- 3. 添加"员工成本列表"子菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('员工成本列表', '', 2, 1, @cost_menu_id, 'list', 'ep:list', 'system/cost/index', 'SystemCostList', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 4. 获取新菜单ID并分配权限
SET @cost_list_menu_id = LAST_INSERT_ID();

-- 5. 将原来成本管理下的"成本查询"权限移动到新的"员工成本列表"下
UPDATE system_menu 
SET parent_id = @cost_list_menu_id 
WHERE name = '成本查询' AND permission = 'system:cost:query' AND deleted = 0;

-- 6. 给管理员角色分配新菜单权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @cost_list_menu_id, '1', NOW(), '1', NOW(), 0, 1
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @cost_list_menu_id AND deleted = 0);

-- 7. 更新职级变更记录的sort顺序
UPDATE system_menu 
SET sort = 2 
WHERE name = '职级变更记录' AND deleted = 0;
