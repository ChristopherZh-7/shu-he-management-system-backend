-- 职级变更记录表
CREATE TABLE IF NOT EXISTS `system_position_level_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `old_position_level` varchar(50) DEFAULT NULL COMMENT '变更前职级',
    `new_position_level` varchar(50) NOT NULL COMMENT '变更后职级',
    `effective_date` date NOT NULL COMMENT '生效日期',
    `change_type` tinyint NOT NULL DEFAULT 1 COMMENT '变更类型：1自动同步 2手动录入',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_effective_date` (`effective_date`),
    KEY `idx_user_effective` (`user_id`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职级变更记录表';

-- 添加菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('职级变更记录', '', 2, 3, (SELECT id FROM (SELECT id FROM system_menu WHERE name = '成本管理' AND deleted = 0 LIMIT 1) tmp), 'position-history', 'ep:timer', 'system/cost/position-history/index', 'SystemPositionHistory', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 添加按钮权限
SET @parentId = (SELECT id FROM system_menu WHERE name = '职级变更记录' AND deleted = 0 LIMIT 1);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('查询职级变更', 'system:position-history:query', 3, 1, @parentId, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('新增职级变更', 'system:position-history:create', 3, 2, @parentId, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('修改职级变更', 'system:position-history:update', 3, 3, @parentId, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('删除职级变更', 'system:position-history:delete', 3, 4, @parentId, '', '', '', '', 0, 1, 1, 1, '1', NOW(), '1', NOW(), 0);

-- 给管理员角色分配权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, id, '1', NOW(), '1', NOW(), 0, 1 FROM system_menu WHERE parent_id = @parentId OR id = @parentId;
