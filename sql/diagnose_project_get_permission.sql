-- 诊断 project/get?id=2 没权限
SELECT 'ay_mg role' AS t;
SELECT id, code FROM system_role WHERE code='ay_mg';

SELECT 'project:project:query menu 5166' AS t;
SELECT id, name, permission FROM system_menu WHERE permission='project:project:query' AND parent_id=5166 AND deleted=0;

SELECT 'ay_mg(163) has project:project:query' AS t;
SELECT srm.role_id, srm.menu_id, sm.permission FROM system_role_menu srm JOIN system_menu sm ON sm.id=srm.menu_id WHERE srm.role_id=163 AND sm.permission='project:project:query' AND srm.deleted=0;

SELECT 'user 226 roles' AS t;
SELECT role_id FROM system_user_role WHERE user_id=226 AND deleted=0;
