SELECT '安全服务(5081) 下的权限' AS info;
SELECT id, name, permission, sort FROM system_menu WHERE parent_id = 5081 AND deleted = 0 ORDER BY sort;

SELECT '安全运营(5166) 下的权限' AS info;
SELECT id, name, permission, sort FROM system_menu WHERE parent_id = 5166 AND deleted = 0 ORDER BY sort;
