-- =============================================
-- 从2月2日备份恢复项目管理模块菜单
-- =============================================

-- 先删除可能存在的残留记录（避免主键冲突）
-- 先删除角色菜单关联
DELETE FROM system_role_menu WHERE menu_id >= 5080 AND menu_id <= 5200;
-- 再删除菜单
DELETE FROM system_menu WHERE id >= 5080 AND id <= 5200;

-- =============================================
-- 1. 项目管理模块主菜单
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5080, '项目管理', '', 1, 70, 0, '/project', 'ep:folder-opened', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 2. 安全服务菜单及权限
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5081, '安全服务', '', 2, 1, 5080, 'security-service', 'ep:lock', 'project/security-service/index', 'ProjectSecurityService', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5082, '项目查询', 'project:info:query', 3, 1, 5081, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5083, '项目创建', 'project:info:create', 3, 2, 5081, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5084, '项目更新', 'project:info:update', 3, 3, 5081, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5085, '项目删除', 'project:info:delete', 3, 4, 5081, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5100, '服务项查询', 'project:service-item:query', 3, 5, 5081, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5101, '服务项创建', 'project:service-item:create', 3, 6, 5081, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5102, '服务项更新', 'project:service-item:update', 3, 7, 5081, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5103, '服务项删除', 'project:service-item:delete', 3, 8, 5081, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 3. 安全运营菜单及权限
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5166, '安全运营', '', 2, 2, 5080, 'security-operation', 'ep:monitor', 'project/security-operation/index', 'SecurityOperationList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5167, '安全运营查询', 'project:security-operation:query', 3, 1, 5166, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5168, '安全运营创建', 'project:security-operation:create', 3, 2, 5166, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5169, '安全运营更新', 'project:security-operation:update', 3, 3, 5166, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5170, '安全运营删除', 'project:security-operation:delete', 3, 4, 5166, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 4. 数据安全菜单及权限
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5091, '数据安全', '', 2, 3, 5080, 'data-security', 'ep:data-analysis', 'project/data-security/index', 'ProjectDataSecurity', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5092, '项目查询', 'project:info:query', 3, 1, 5091, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5093, '项目创建', 'project:info:create', 3, 2, 5091, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5094, '项目更新', 'project:info:update', 3, 3, 5091, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5095, '项目删除', 'project:info:delete', 3, 4, 5091, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5108, '项目查询', 'project:info:query', 3, 1, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5109, '项目创建', 'project:info:create', 3, 2, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5110, '项目更新', 'project:info:update', 3, 3, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5111, '项目删除', 'project:info:delete', 3, 4, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5112, '服务项查询', 'project:service-item:query', 3, 5, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5113, '服务项创建', 'project:service-item:create', 3, 6, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5114, '服务项更新', 'project:service-item:update', 3, 7, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5115, '服务项删除', 'project:service-item:delete', 3, 8, 5091, '', '', '', '', 0, b'1', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 5. 项目详情和轮次详情（隐藏菜单）
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5096, '项目详情', '', 2, 99, 5080, 'detail/:id', '', 'project/detail/index', 'ProjectDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5099, '轮次详情', '', 2, 100, 5080, 'round/:roundId', '', 'project/round/index', 'ProjectRoundDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5172, '安全运营详情', '', 2, 99, 5080, 'security-operation/detail/:id', '', 'project/security-operation/detail', 'SecurityOperationDetail', 0, b'0', b'1', b'0', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 6. 成本管理模块
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5116, '成本管理', '', 1, 5, 0, 'cost-management', 'ep:money', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5117, '成本查询', 'system:cost:query', 3, 1, 5116, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5123, '员工成本列表', '', 2, 1, 5116, 'user-cost', 'ep:list', 'cost-management/index', 'SystemCostList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5118, '职级变更记录', '', 2, 3, 5116, 'position-history', 'ep:timer', 'cost-management/position-history/index', 'SystemPositionHistory', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5119, '查询职级变更', 'system:position-history:query', 3, 1, 5118, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5120, '新增职级变更', 'system:position-history:create', 3, 2, 5118, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5121, '修改职级变更', 'system:position-history:update', 3, 3, 5118, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5122, '删除职级变更', 'system:position-history:delete', 3, 4, 5118, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5147, '合同收入分配', '', 2, 3, 5116, 'contract-allocation', 'ep:money', 'cost-management/contract-allocation/index', 'ContractAllocation', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5148, '合同分配查询', 'system:contract-allocation:query', 3, 1, 5147, '', '', '', '', 0, b'1', b'0', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5149, '合同分配创建', 'system:contract-allocation:create', 3, 2, 5147, '', '', '', '', 0, b'1', b'0', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5150, '合同分配更新', 'system:contract-allocation:update', 3, 3, 5147, '', '', '', '', 0, b'1', b'0', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5151, '合同分配删除', 'system:contract-allocation:delete', 3, 4, 5147, '', '', '', '', 0, b'1', b'0', b'0', 'admin', NOW(), 'admin', NOW(), b'0'),
(5152, '跨部门项目费用', '', 2, 4, 5116, 'outside-cost', 'ep:tickets', 'cost-management/outside-cost/index', 'OutsideCost', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5153, '跨部门项目费用查询', 'system:outside-cost:query', 3, 1, 5152, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5154, '指派结算人', 'system:outside-cost:assign', 3, 2, 5152, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5155, '填写费用金额', 'system:outside-cost:fill', 3, 3, 5152, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5187, '经营分析', '', 2, 60, 5116, 'business-analysis', 'ep:data-analysis', 'cost-management/business-analysis/index', 'BusinessAnalysis', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5188, '经营分析查询', 'system:business-analysis:query', 3, 1, 5187, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 7. 服务项发起模块
-- =============================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(5131, '服务项发起', '', 1, 2, 1185, 'service-initiate', 'ep:promotion', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5156, '服务发起', '', 2, 0, 5131, 'service-launch', 'ep:promotion', 'project/service-launch/index', 'ServiceLaunchList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5157, '发起服务', 'project:service-launch:create', 3, 1, 5156, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5158, '查看服务发起', 'project:service-launch:query', 3, 2, 5156, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
(5159, '删除服务发起', 'project:service-launch:delete', 3, 3, 5156, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- =============================================
-- 8. 为超级管理员分配菜单权限
-- =============================================
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE id IN (5080,5081,5082,5083,5084,5085,5091,5092,5093,5094,5095,5096,5099,5100,5101,5102,5103,5108,5109,5110,5111,5112,5113,5114,5115,5116,5117,5118,5119,5120,5121,5122,5123,5131,5147,5148,5149,5150,5151,5152,5153,5154,5155,5156,5157,5158,5159,5166,5167,5168,5169,5170,5172,5187,5188)
  AND deleted = 0;

-- =============================================
-- 9. 验证恢复结果
-- =============================================
SELECT '===== 项目管理模块恢复完成 =====' AS title;

SELECT 
    m.id,
    m.name,
    m.path,
    m.parent_id,
    CASE m.type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END AS menu_type
FROM `system_menu` m
WHERE m.id IN (5080, 5081, 5091, 5096, 5099, 5116, 5131, 5156, 5166, 5172)
  AND m.deleted = 0
ORDER BY m.id;

SELECT CONCAT('恢复完成！项目管理模块菜单数量: ', 
    (SELECT COUNT(*) FROM system_menu WHERE id >= 5080 AND id <= 5200 AND deleted = 0)
) AS result;
