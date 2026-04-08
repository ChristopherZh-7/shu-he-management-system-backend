-- 财务管理模块：添加菜单和权限配置

-- 一级目录：财务管理
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5300, '财务管理', '', 1, 6, 0, '/finance', 'ep:money', NULL, NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 二级菜单：项目预算管理
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5301, '项目预算管理', '', 2, 1, 5300, 'budget', 'ep:coin', 'finance/budget/index', 'FinanceBudget', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 三级按钮：项目预算的增删改查
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5302, '项目预算查询', 'finance:project-budget:query', 3, 1, 5301, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5303, '项目预算创建', 'finance:project-budget:create', 3, 2, 5301, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5304, '项目预算修改', 'finance:project-budget:update', 3, 3, 5301, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5305, '项目预算删除', 'finance:project-budget:delete', 3, 4, 5301, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 二级菜单：服务项收入分配
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5310, '收入分配管理', '', 2, 2, 5300, 'allocation', 'ep:operation', 'finance/allocation/index', 'FinanceAllocation', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 三级按钮：服务项收入分配的增删改查
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5311, '收入分配查询', 'finance:service-allocation:query', 3, 1, 5310, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5312, '收入分配创建', 'finance:service-allocation:create', 3, 2, 5310, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5313, '收入分配修改', 'finance:service-allocation:update', 3, 3, 5310, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (5314, '收入分配删除', 'finance:service-allocation:delete', 3, 4, 5310, '', '', '', NULL, 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 给超级管理员角色（id=1）分配所有财务菜单权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, id, '1', NOW(), '1', NOW(), 0, 1 FROM system_menu WHERE id BETWEEN 5300 AND 5314;
