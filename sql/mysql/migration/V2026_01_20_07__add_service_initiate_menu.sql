-- 在「工作流程」菜单(ID=1185)下添加「服务项发起」目录
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('服务项发起', '', 1, 2, 1185, 'service-initiate', 'ep:promotion', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

SET @service_initiate_menu_id = LAST_INSERT_ID();

-- 将「渗透测试发起」移动到「服务项发起」目录下
UPDATE system_menu 
SET parent_id = @service_initiate_menu_id, 
    sort = 1,
    updater = '1',
    update_time = NOW()
WHERE parent_id = 1185 
AND path = 'penetration' 
AND deleted = b'0';

-- 为超级管理员角色分配「服务项发起」目录权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @service_initiate_menu_id, '1', NOW(), '1', NOW(), b'0', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @service_initiate_menu_id AND deleted = b'0');
