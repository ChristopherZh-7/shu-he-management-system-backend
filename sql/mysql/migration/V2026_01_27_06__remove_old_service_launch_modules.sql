-- 删除服务执行发起和外出请求发起相关菜单
-- 这些功能已被统一服务发起功能替代

-- 1. 删除服务执行发起相关菜单
DELETE FROM system_menu WHERE name = '服务执行发起';
DELETE FROM system_menu WHERE name = '发起服务执行';
DELETE FROM system_menu WHERE name = '服务执行详情';

-- 2. 删除外出请求发起相关菜单
DELETE FROM system_menu WHERE name = '外出请求发起';
DELETE FROM system_menu WHERE name = '发起外出请求';
DELETE FROM system_menu WHERE name = '外出请求详情';
DELETE FROM system_menu WHERE name = '外出服务详情';

-- 3. 删除外出请求相关表（如果存在）
-- 注意：如果这些表中有重要历史数据，可以选择保留表但删除菜单
-- DROP TABLE IF EXISTS project_outside_member;
-- DROP TABLE IF EXISTS project_outside_request;

-- 4. 删除服务执行相关表（如果存在）
-- DROP TABLE IF EXISTS project_service_execution;

-- 备注：
-- - project_outside_member 表结构已被 project_service_launch_member 替代
-- - project_outside_request 表结构已被 project_service_launch 替代
-- - project_service_execution 表结构已被 project_service_launch 替代
-- 如需保留历史数据，可以暂时保留这些表，后续进行数据迁移
