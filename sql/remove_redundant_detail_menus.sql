-- =============================================
-- 删除冗余的隐藏菜单：项目详情、轮次详情、安全运营详情
--
-- 原因：这三个路由已在前端 project.ts 中静态定义，
--       不再依赖后端菜单，删除可简化权限树。
-- =============================================

SET NAMES utf8mb4;

-- 1. 先删除角色-菜单关联（避免残留）
UPDATE system_role_menu rm
JOIN system_menu m ON m.id = rm.menu_id AND m.name IN ('项目详情', '轮次详情', '安全运营详情') AND m.deleted = 0
SET rm.deleted = 1, rm.updater = 'admin', rm.update_time = NOW()
WHERE rm.deleted = 0;

-- 2. 软删除菜单（deleted = 1）
UPDATE system_menu SET deleted = 1, updater = 'admin', update_time = NOW()
WHERE name IN ('项目详情', '轮次详情', '安全运营详情') AND deleted = 0;

SELECT '已删除 项目详情、轮次详情、安全运营详情 菜单' AS result;
