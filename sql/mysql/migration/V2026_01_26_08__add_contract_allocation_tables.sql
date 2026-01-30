-- =====================================================
-- 合同收入分配功能
-- 两级分配：合同 → 部门 → 服务项
-- =====================================================

-- 1. 创建合同部门分配表
CREATE TABLE IF NOT EXISTS `contract_dept_allocation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `contract_id` bigint NOT NULL COMMENT 'CRM合同ID',
    `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号（冗余）',
    `customer_name` varchar(128) DEFAULT NULL COMMENT '客户名称（冗余）',
    `dept_id` bigint NOT NULL COMMENT '部门ID',
    `dept_name` varchar(64) DEFAULT NULL COMMENT '部门名称（冗余）',
    `allocated_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '分配金额',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_dept_id` (`dept_id`),
    UNIQUE KEY `uk_contract_dept` (`contract_id`, `dept_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同部门分配表';

-- 2. 创建服务项金额分配表
CREATE TABLE IF NOT EXISTS `service_item_allocation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `contract_dept_allocation_id` bigint NOT NULL COMMENT '合同部门分配ID',
    `service_item_id` bigint NOT NULL COMMENT '服务项ID',
    `service_item_name` varchar(128) DEFAULT NULL COMMENT '服务项名称（冗余）',
    `allocated_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '分配金额',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    KEY `idx_contract_dept_allocation_id` (`contract_dept_allocation_id`),
    KEY `idx_service_item_id` (`service_item_id`),
    UNIQUE KEY `uk_allocation_service_item` (`contract_dept_allocation_id`, `service_item_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务项金额分配表';

-- 3. 添加菜单
-- 获取成本管理菜单ID
SET @cost_menu_id = (SELECT id FROM system_menu WHERE name = '成本管理' AND deleted = 0 LIMIT 1);

-- 添加"合同收入分配"菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('合同收入分配', '', 2, 3, @cost_menu_id, 'contract-allocation', 'ep:money', 'cost-management/contract-allocation/index', 'ContractAllocation', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

SET @allocation_menu_id = LAST_INSERT_ID();

-- 添加按钮权限
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES 
('合同分配查询', 'system:contract-allocation:query', 3, 1, @allocation_menu_id, '', '', '', '', 0, 1, 0, 0, '1', NOW(), '1', NOW(), 0),
('合同分配创建', 'system:contract-allocation:create', 3, 2, @allocation_menu_id, '', '', '', '', 0, 1, 0, 0, '1', NOW(), '1', NOW(), 0),
('合同分配更新', 'system:contract-allocation:update', 3, 3, @allocation_menu_id, '', '', '', '', 0, 1, 0, 0, '1', NOW(), '1', NOW(), 0),
('合同分配删除', 'system:contract-allocation:delete', 3, 4, @allocation_menu_id, '', '', '', '', 0, 1, 0, 0, '1', NOW(), '1', NOW(), 0);

-- 给管理员角色分配菜单权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, id, '1', NOW(), '1', NOW(), 0, 1
FROM system_menu 
WHERE (id = @allocation_menu_id OR parent_id = @allocation_menu_id) AND deleted = 0
AND NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = system_menu.id AND deleted = 0);
