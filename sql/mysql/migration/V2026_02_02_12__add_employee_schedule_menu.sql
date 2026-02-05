-- 添加员工工作状态和服务进度监控菜单

-- 先查找服务项发起的父菜单ID（假设在项目管理下）
-- 根据现有结构，添加新菜单

-- 员工工作状态菜单
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (3100, '员工工作状态', '', 2, 10, 0, 'employee-schedule', 'ep:user-filled', 'project/employee-schedule/index', 'EmployeeSchedule', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

-- 服务进度监控菜单
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (3101, '服务进度监控', '', 2, 11, 0, 'service-progress', 'ep:data-analysis', 'project/service-progress/index', 'ServiceProgress', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

-- 员工排期查询权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (3102, '员工排期查询', 'project:employee-schedule:query', 3, 1, 3100, '', '', '', '', 0, 1, 1, 1, 'admin', NOW(), 'admin', NOW(), 0);

-- 更新服务发起菜单名称为"跨部门服务申请"
UPDATE system_menu SET name = '跨部门服务申请' WHERE component_name = 'ServiceLaunchList' OR name = '服务发起';
