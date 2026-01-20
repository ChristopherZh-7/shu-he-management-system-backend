-- 删除旧的「项目审批」菜单及其子菜单和关联的角色权限
-- 先删除角色菜单关联
DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu 
    WHERE parent_id IN (
        SELECT id FROM system_menu WHERE path = '/project-approval' AND parent_id = 0
    )
);

DELETE FROM system_role_menu 
WHERE menu_id IN (
    SELECT id FROM system_menu WHERE path = '/project-approval' AND parent_id = 0
);

-- 删除子菜单的按钮权限
DELETE FROM system_menu 
WHERE parent_id IN (
    SELECT id FROM (
        SELECT id FROM system_menu 
        WHERE parent_id IN (
            SELECT id FROM system_menu WHERE path = '/project-approval' AND parent_id = 0
        )
    ) AS tmp
);

-- 删除子菜单
DELETE FROM system_menu 
WHERE parent_id IN (
    SELECT id FROM (
        SELECT id FROM system_menu WHERE path = '/project-approval' AND parent_id = 0
    ) AS tmp
);

-- 删除项目审批一级菜单
DELETE FROM system_menu WHERE path = '/project-approval' AND parent_id = 0;

-- 在「工作流程」菜单(ID=1185)下添加「渗透测试发起」子菜单（如果不存在）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '渗透测试发起', '', 2, 10, 1185, 'penetration', 'ep:aim', 'project/penetration/index', 'PenetrationTestList', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE parent_id = 1185 AND path = 'penetration' AND deleted = b'0'
);

SET @penetration_menu_id = (SELECT id FROM system_menu WHERE parent_id = 1185 AND path = 'penetration' AND deleted = b'0' LIMIT 1);

-- 添加渗透测试发起的权限按钮（如果不存在）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '发起渗透测试', 'project:penetration:create', 3, 1, @penetration_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @penetration_menu_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @penetration_menu_id AND permission = 'project:penetration:create' AND deleted = b'0');

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '查看渗透测试', 'project:penetration:query', 3, 2, @penetration_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @penetration_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE parent_id = @penetration_menu_id AND permission = 'project:penetration:query' AND deleted = b'0');

-- 为超级管理员角色分配权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @penetration_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE @penetration_menu_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @penetration_menu_id AND deleted = b'0');
