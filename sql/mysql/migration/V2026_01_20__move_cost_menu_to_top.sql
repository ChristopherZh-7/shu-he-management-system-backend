-- 将成本管理菜单移到最外层（顶级菜单）

-- 1. 更新成本管理菜单为顶级菜单（parent_id = 0）
UPDATE system_menu 
SET parent_id = 0, 
    sort = 5,  -- 设置排序，放在比较靠前的位置
    path = 'cost-management',  -- 更新路径
    component = ''  -- 顶级目录菜单不需要 component
WHERE name = '成本管理' AND deleted = 0;

-- 2. 更新员工成本列表的路径和组件
UPDATE system_menu 
SET path = 'user-cost',
    component = 'cost-management/index'
WHERE name = '员工成本列表' AND deleted = 0;

-- 3. 更新职级变更记录的路径和组件
UPDATE system_menu 
SET path = 'position-history',
    component = 'cost-management/position-history/index'
WHERE name = '职级变更记录' AND deleted = 0;

-- 4. 更新成本查询权限的路径
UPDATE system_menu 
SET component = ''
WHERE name LIKE 'system:cost:%' AND deleted = 0;

-- 5. 更新职级变更记录权限的路径
UPDATE system_menu 
SET component = ''
WHERE name LIKE 'system:position-history:%' AND deleted = 0;
