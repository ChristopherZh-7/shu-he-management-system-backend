-- ========================================
-- 删除项目管理模块菜单（硬删除）
-- 说明: 彻底删除项目管理及其所有子菜单
-- 注意: 此操作不可逆，请确保已备份！
-- ========================================

-- 1. 获取项目管理菜单ID
SET @project_menu_id = (SELECT id FROM system_menu WHERE name = '项目管理' AND parent_id = 0 LIMIT 1);

-- 2. 先清理角色与菜单的关联关系
DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu 
    WHERE parent_id IN (
        SELECT id FROM (SELECT id FROM system_menu WHERE parent_id = @project_menu_id) AS sub
    )
);

DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu WHERE parent_id = @project_menu_id
);

DELETE FROM system_role_menu 
WHERE menu_id = @project_menu_id;

-- 清理路径包含 project 的菜单关联
DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu 
    WHERE path LIKE '%/project%' 
       OR path LIKE 'project%'
       OR component LIKE 'project/%'
       OR name LIKE '%项目管理%'
       OR path LIKE '%project-management%'
);

-- 3. 硬删除项目管理的所有子菜单（从最底层开始）
-- 第三层子菜单（权限按钮等）
DELETE FROM system_menu 
WHERE parent_id IN (
    SELECT id FROM (
        SELECT id FROM system_menu WHERE parent_id = @project_menu_id
    ) AS sub
);

-- 第二层子菜单
DELETE FROM system_menu 
WHERE parent_id = @project_menu_id;

-- 4. 硬删除项目管理主菜单
DELETE FROM system_menu 
WHERE id = @project_menu_id;

-- 5. 删除所有路径包含 project 的菜单（确保清理干净）
DELETE FROM system_menu 
WHERE path LIKE '%/project%' 
   OR path LIKE 'project%'
   OR component LIKE 'project/%';

-- 6. 删除工作台中项目管理相关的菜单
DELETE FROM system_menu 
WHERE name LIKE '%项目管理%' 
   OR path LIKE '%project-management%';

-- 7. 验证删除结果（应该返回空）
SELECT id, name, path, parent_id, deleted 
FROM system_menu 
WHERE name LIKE '%项目%' 
   OR path LIKE '%project%'
ORDER BY parent_id, id;
