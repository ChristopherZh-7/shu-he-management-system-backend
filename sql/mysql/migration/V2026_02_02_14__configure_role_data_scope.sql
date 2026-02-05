-- =============================================
-- 角色权限完整配置脚本
-- 包含：数据范围 + 菜单权限
-- =============================================

-- =============================================
-- 第一部分：更新数据范围 (dataScope)
-- =============================================
-- 数据范围说明：
-- 1 = ALL (全部数据)
-- 2 = DEPT_CUSTOM (指定部门)
-- 3 = DEPT_ONLY (本部门)
-- 4 = DEPT_AND_CHILD (本部门及以下)
-- 5 = SELF (仅本人)

-- 主管级别：本部门及以下
UPDATE system_role SET data_scope = 4 WHERE code = 'sh_mg';  -- 数据安全主管
UPDATE system_role SET data_scope = 4 WHERE code = 'ay_mg';  -- 安全运营主管
UPDATE system_role SET data_scope = 4 WHERE code = 'af_mg';  -- 安全服务主管

-- 组长级别：本部门
UPDATE system_role SET data_scope = 3 WHERE code = 'sh_tl';  -- 数据安全组长
UPDATE system_role SET data_scope = 3 WHERE code = 'ay_tl';  -- 安全运营组长
UPDATE system_role SET data_scope = 3 WHERE code = 'af_tl';  -- 安全服务组长

-- 工程师级别：仅本人
UPDATE system_role SET data_scope = 5 WHERE code = 'sh_emp'; -- 数据安全工程师
UPDATE system_role SET data_scope = 5 WHERE code = 'ay_emp'; -- 安全运营工程师
UPDATE system_role SET data_scope = 5 WHERE code = 'af_emp'; -- 安全服务工程师

-- =============================================
-- 第二部分：清理旧的角色菜单关联
-- =============================================
DELETE FROM system_role_menu WHERE role_id IN (
    SELECT id FROM system_role WHERE code IN ('sh_mg', 'ay_mg', 'af_mg', 'sh_tl', 'ay_tl', 'af_tl', 'sh_emp', 'ay_emp', 'af_emp')
);

-- =============================================
-- 第三部分：分配菜单权限
-- =============================================

-- ==================== 安全服务主管 (af_mg) ====================
-- 完整权限：项目CRUD + 服务发起CRUD + 工作记录CRUD + 成本管理CRUD
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'af_mg' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080,  -- 项目管理
    5081,  -- 安全服务
    5096,  -- 项目详情
    5099,  -- 轮次详情
    5116,  -- 成本管理
    5179,  -- 工作台
    5131,  -- 服务项发起
    5156,  -- 服务发起
    -- 项目管理权限
    5082, 5083, 5084, 5085,  -- project:info (query,create,update,delete)
    5100, 5101, 5102, 5103,  -- project:service-item (query,create,update,delete)
    -- 服务发起权限
    5157, 5158, 5159,  -- service-launch (create,query,delete)
    -- 工作记录权限
    5173,  -- 项目管理记录菜单
    5174, 5175, 5176, 5177, 5178,  -- management-record (query,create,update,delete,export)
    5180,  -- 日常管理记录菜单
    5181, 5182, 5183, 5184,  -- daily-record (query,create,update,delete)
    5185, 5186,  -- 周工作日历
    -- 成本管理权限
    5117,  -- 成本查询
    5147, 5148, 5149, 5150, 5151,  -- 合同分配 (menu + query,create,update,delete)
    5152, 5153, 5154, 5155,  -- 跨部门费用 (menu + query,assign,fill)
    5187, 5188  -- 经营分析
);

-- ==================== 安全运营主管 (ay_mg) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'ay_mg' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080,  -- 项目管理
    5166,  -- 安全运营
    5172,  -- 安全运营详情
    5116,  -- 成本管理
    5179,  -- 工作台
    5131,  -- 服务项发起
    5156,  -- 服务发起
    -- 安全运营权限
    5167, 5168, 5169, 5170,  -- security-operation (query,create,update,delete)
    -- 项目只读
    5082,  -- project:info:query (安全服务下)
    5087,  -- project:info:query (项目详情下)
    -- 服务发起权限
    5157, 5158, 5159,  -- service-launch (create,query,delete)
    -- 工作记录权限
    5173, 5174, 5175, 5176, 5177, 5178,
    5180, 5181, 5182, 5183, 5184,
    5185, 5186,
    -- 成本管理权限
    5117, 5147, 5148, 5149, 5150, 5151,
    5152, 5153, 5154, 5155, 5187, 5188
);

-- ==================== 数据安全主管 (sh_mg) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'sh_mg' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080,  -- 项目管理
    5091,  -- 数据安全
    5096,  -- 项目详情
    5099,  -- 轮次详情
    5116,  -- 成本管理
    5179,  -- 工作台
    5131,  -- 服务项发起
    5156,  -- 服务发起
    -- 项目管理权限（数据安全下）
    5092, 5093, 5094, 5095,  -- project:info (query,create,update,delete)
    5108, 5109, 5110, 5111,  -- project:info 重复（兼容）
    5112, 5113, 5114, 5115,  -- project:service-item (query,create,update,delete)
    -- 服务发起权限
    5157, 5158, 5159,
    -- 工作记录权限
    5173, 5174, 5175, 5176, 5177, 5178,
    5180, 5181, 5182, 5183, 5184,
    5185, 5186,
    -- 成本管理权限
    5117, 5147, 5148, 5149, 5150, 5151,
    5152, 5153, 5154, 5155, 5187, 5188
);

-- ==================== 安全服务组长 (af_tl) ====================
-- 部分权限：无删除、成本只读
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'af_tl' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5081, 5096, 5099, 5116, 5179, 5131, 5156,
    -- 项目管理权限（无删除）
    5082, 5083, 5084,  -- project:info (query,create,update)
    5100, 5101, 5102,  -- project:service-item (query,create,update)
    -- 服务发起权限（无删除）
    5157, 5158,  -- service-launch (create,query)
    -- 工作记录权限（无删除）
    5173, 5174, 5175, 5176, 5178,  -- 无 5177(delete)
    5180, 5181, 5182, 5183,  -- 无 5184(delete)
    5185, 5186,
    -- 成本管理权限（只读）
    5117, 5147, 5148, 5152, 5153
);

-- ==================== 安全运营组长 (ay_tl) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'ay_tl' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5166, 5172, 5116, 5179, 5131, 5156,
    -- 安全运营权限（无删除）
    5167, 5168, 5169,  -- security-operation (query,create,update)
    -- 项目只读
    5082, 5087,
    -- 服务发起权限（无删除）
    5157, 5158,
    -- 工作记录权限（无删除）
    5173, 5174, 5175, 5176, 5178,
    5180, 5181, 5182, 5183,
    5185, 5186,
    -- 成本管理权限（只读）
    5117, 5147, 5148, 5152, 5153
);

-- ==================== 数据安全组长 (sh_tl) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'sh_tl' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5091, 5096, 5099, 5116, 5179, 5131, 5156,
    -- 项目管理权限（无删除）
    5092, 5093, 5094, 5108, 5109, 5110,  -- project:info (query,create,update)
    5112, 5113, 5114,  -- project:service-item (query,create,update)
    -- 服务发起权限（无删除）
    5157, 5158,
    -- 工作记录权限（无删除）
    5173, 5174, 5175, 5176, 5178,
    5180, 5181, 5182, 5183,
    5185, 5186,
    -- 成本管理权限（只读）
    5117, 5147, 5148, 5152, 5153
);

-- ==================== 安全服务工程师 (af_emp) ====================
-- 最小权限：只读 + 发起 + 自己的工作记录
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'af_emp' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5081, 5179, 5131, 5156,
    -- 项目只读
    5082,  -- project:info:query
    5100,  -- project:service-item:query
    -- 服务发起（创建+查看）
    5157, 5158,
    -- 工作记录（自己的）
    5173, 5174, 5175, 5176,  -- query,create,update (数据权限会限制只看自己的)
    5180, 5181, 5182, 5183,
    5185, 5186
);

-- ==================== 安全运营工程师 (ay_emp) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'ay_emp' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5166, 5179, 5131, 5156,
    -- 安全运营只读
    5167,  -- security-operation:query
    -- 项目只读
    5082, 5087,
    -- 服务发起（创建+查看）
    5157, 5158,
    -- 工作记录（自己的）
    5173, 5174, 5175, 5176,
    5180, 5181, 5182, 5183,
    5185, 5186
);

-- ==================== 数据安全工程师 (sh_emp) ====================
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM system_role r, system_menu m
WHERE r.code = 'sh_emp' AND m.deleted = 0 AND m.id IN (
    -- 目录菜单
    5080, 5091, 5179, 5131, 5156,
    -- 项目只读
    5092, 5108, 5112,  -- project:info:query, service-item:query
    -- 服务发起（创建+查看）
    5157, 5158,
    -- 工作记录（自己的）
    5173, 5174, 5175, 5176,
    5180, 5181, 5182, 5183,
    5185, 5186
);

-- =============================================
-- 第四部分：验证结果
-- =============================================
SELECT 
    r.name AS 角色名称,
    r.code AS 角色编码,
    r.data_scope AS 数据范围,
    CASE r.data_scope
        WHEN 1 THEN '全部数据'
        WHEN 2 THEN '指定部门'
        WHEN 3 THEN '本部门'
        WHEN 4 THEN '本部门及以下'
        WHEN 5 THEN '仅本人'
        ELSE '未知'
    END AS 数据范围说明,
    COUNT(rm.menu_id) AS 菜单数量
FROM system_role r
LEFT JOIN system_role_menu rm ON r.id = rm.role_id AND rm.deleted = 0
WHERE r.code IN ('sh_mg', 'ay_mg', 'af_mg', 'sh_tl', 'ay_tl', 'af_tl', 'sh_emp', 'ay_emp', 'af_emp')
GROUP BY r.id, r.name, r.code, r.data_scope
ORDER BY 
    CASE 
        WHEN r.code LIKE '%_mg' THEN 1
        WHEN r.code LIKE '%_tl' THEN 2
        WHEN r.code LIKE '%_emp' THEN 3
    END,
    r.code;
