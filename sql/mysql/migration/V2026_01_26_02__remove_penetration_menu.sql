-- =====================================================
-- 删除渗透测试发起相关菜单和权限
-- =====================================================

-- 获取渗透测试菜单ID（现在在「服务项发起」目录下）
SET @penetration_menu_id = (
    SELECT id FROM system_menu 
    WHERE path = 'penetration' 
    AND deleted = b'0' 
    LIMIT 1
);

-- 删除渗透测试菜单下的按钮权限的角色关联
DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu 
    WHERE parent_id = @penetration_menu_id 
    AND deleted = b'0'
);

-- 删除渗透测试菜单的角色关联
DELETE FROM system_role_menu 
WHERE menu_id = @penetration_menu_id;

-- 删除渗透测试菜单下的按钮权限
UPDATE system_menu 
SET deleted = b'1', 
    updater = '1', 
    update_time = NOW() 
WHERE parent_id = @penetration_menu_id 
AND deleted = b'0';

-- 删除渗透测试菜单本身
UPDATE system_menu 
SET deleted = b'1', 
    updater = '1', 
    update_time = NOW() 
WHERE id = @penetration_menu_id 
AND deleted = b'0';
