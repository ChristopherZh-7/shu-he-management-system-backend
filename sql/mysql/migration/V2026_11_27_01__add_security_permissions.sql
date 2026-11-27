-- 允许为业务角色单独分配上传和下拉选项权限，无需授予完整的系统管理查询权限。
INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT additions.name, additions.permission, 3, 2, menu.parent_id, '', '', '', NULL,
       0, b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0'
FROM (
    SELECT '文件上传' AS name, 'infra:file:upload' AS permission, 'infra:file:query' AS reference_permission
    UNION ALL SELECT '员工下拉选项', 'system:user:lookup', 'system:user:query'
    UNION ALL SELECT '部门下拉选项', 'system:dept:lookup', 'system:dept:query'
    UNION ALL SELECT '角色下拉选项', 'system:role:lookup', 'system:role:query'
) additions
JOIN system_menu menu ON menu.id = (
    SELECT MIN(reference_menu.id) FROM system_menu reference_menu
    WHERE reference_menu.permission = additions.reference_permission AND reference_menu.deleted = b'0'
)
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu existing
    WHERE existing.permission = additions.permission AND existing.deleted = b'0'
);
