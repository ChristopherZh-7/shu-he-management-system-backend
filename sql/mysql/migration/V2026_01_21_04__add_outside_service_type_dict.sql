-- 为各部门类型的服务类型字典添加"外出服务"类型

-- 安全服务部门 (project_service_type_security)
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 100, '外出服务', 'outside', 'project_service_type_security', 0, 'warning', '', '合同领取时自动创建的外出服务项', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data 
    WHERE dict_type = 'project_service_type_security' AND value = 'outside' AND deleted = 0
);

-- 安全运营部门 (project_service_type_operation)
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 100, '外出服务', 'outside', 'project_service_type_operation', 0, 'warning', '', '合同领取时自动创建的外出服务项', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data 
    WHERE dict_type = 'project_service_type_operation' AND value = 'outside' AND deleted = 0
);

-- 数据安全部门 (project_service_type_data)
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT 100, '外出服务', 'outside', 'project_service_type_data', 0, 'warning', '', '合同领取时自动创建的外出服务项', '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data 
    WHERE dict_type = 'project_service_type_data' AND value = 'outside' AND deleted = 0
);
