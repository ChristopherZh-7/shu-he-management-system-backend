-- 外出费用记录表
CREATE TABLE IF NOT EXISTS `outside_cost_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `outside_request_id` bigint NOT NULL COMMENT '外出申请ID',
    `contract_id` bigint NOT NULL COMMENT '合同ID',
    `contract_no` varchar(64) DEFAULT NULL COMMENT '合同编号（快照）',
    `contract_name` varchar(255) DEFAULT NULL COMMENT '合同名称（快照）',
    `service_item_id` bigint DEFAULT NULL COMMENT '服务项ID',
    `service_item_name` varchar(255) DEFAULT NULL COMMENT '服务项名称（快照）',
    `request_dept_id` bigint DEFAULT NULL COMMENT '发起部门ID（A部门）',
    `request_dept_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称（快照）',
    `request_user_id` bigint DEFAULT NULL COMMENT '发起人ID',
    `request_user_name` varchar(64) DEFAULT NULL COMMENT '发起人姓名（快照）',
    `target_dept_id` bigint DEFAULT NULL COMMENT '目标部门ID（B部门）',
    `target_dept_name` varchar(100) DEFAULT NULL COMMENT '目标部门名称（快照）',
    `amount` decimal(12,2) DEFAULT NULL COMMENT '费用金额',
    `settle_user_id` bigint DEFAULT NULL COMMENT '结算人ID（找谁要钱）',
    `settle_user_name` varchar(64) DEFAULT NULL COMMENT '结算人姓名（快照）',
    `settle_dept_id` bigint DEFAULT NULL COMMENT '结算人部门ID',
    `settle_dept_name` varchar(100) DEFAULT NULL COMMENT '结算人部门名称（快照）',
    `assign_user_id` bigint DEFAULT NULL COMMENT '指派人ID（B部门负责人，选择结算人的人）',
    `assign_user_name` varchar(64) DEFAULT NULL COMMENT '指派人姓名（快照）',
    `assign_time` datetime DEFAULT NULL COMMENT '指派时间',
    `fill_user_id` bigint DEFAULT NULL COMMENT '填写人ID（结算人填写金额）',
    `fill_user_name` varchar(64) DEFAULT NULL COMMENT '填写人姓名（快照）',
    `fill_time` datetime DEFAULT NULL COMMENT '填写时间',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待指派结算人 1-待填写金额 2-已完成',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_outside_request_id` (`outside_request_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_target_dept_id` (`target_dept_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外出费用记录表';

-- 获取成本管理菜单ID
SET @cost_menu_id = (SELECT id FROM system_menu WHERE name = '成本管理' AND deleted = 0 LIMIT 1);

-- 添加"跨部门项目费用"菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('跨部门项目费用', '', 2, 4, @cost_menu_id, 'outside-cost', 'ep:tickets', 'cost-management/outside-cost/index', 'OutsideCost', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 获取新增菜单的ID
SET @outside_cost_menu_id = LAST_INSERT_ID();

-- 添加按钮权限
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('跨部门项目费用查询', 'system:outside-cost:query', 3, 1, @outside_cost_menu_id, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('指派结算人', 'system:outside-cost:assign', 3, 2, @outside_cost_menu_id, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('填写费用金额', 'system:outside-cost:fill', 3, 3, @outside_cost_menu_id, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 为超级管理员角色分配权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, @outside_cost_menu_id, '1', NOW(), '1', NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @outside_cost_menu_id AND deleted = 0);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, id, '1', NOW(), '1', NOW(), 0
FROM system_menu WHERE parent_id = @outside_cost_menu_id AND deleted = 0;
