-- ==================================
-- 工单模块数据库表
-- ==================================

-- ----------------------------
-- 工单分类表
-- ----------------------------
DROP TABLE IF EXISTS `ticket_category`;
CREATE TABLE `ticket_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类ID',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-禁用 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类表';

-- 插入默认分类
INSERT INTO `ticket_category` (`name`, `parent_id`, `sort`, `status`) VALUES 
('IT支持', 0, 1, 0),
('网络故障', 1, 1, 0),
('设备维修', 1, 2, 0),
('软件问题', 1, 3, 0),
('账号问题', 1, 4, 0),
('行政事务', 0, 2, 0),
('报销申请', 6, 1, 0),
('办公用品', 6, 2, 0),
('其他', 0, 99, 0);

-- ----------------------------
-- 工单表
-- ----------------------------
DROP TABLE IF EXISTS `ticket_ticket`;
CREATE TABLE `ticket_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `ticket_no` varchar(50) NOT NULL COMMENT '工单编号',
  `title` varchar(200) NOT NULL COMMENT '工单标题',
  `description` text COMMENT '工单描述',
  `category_id` bigint DEFAULT NULL COMMENT '工单分类ID',
  `priority` tinyint NOT NULL DEFAULT 1 COMMENT '优先级：0-低 1-普通 2-高 3-紧急',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-已分配 2-处理中 3-待确认 4-已完成 5-已关闭 6-已取消',
  `creator_id` bigint NOT NULL COMMENT '创建人ID（发起人）',
  `assignee_id` bigint DEFAULT NULL COMMENT '处理人ID',
  `customer_id` bigint DEFAULT NULL COMMENT '关联客户ID',
  `expect_time` datetime DEFAULT NULL COMMENT '期望完成时间',
  `finish_time` datetime DEFAULT NULL COMMENT '实际完成时间',
  `attachments` varchar(2000) DEFAULT NULL COMMENT '附件URL（JSON数组）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- ----------------------------
-- 工单操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `ticket_log`;
CREATE TABLE `ticket_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型：create-创建 assign-分配 process-处理 finish-完成 close-关闭 cancel-取消 comment-评论',
  `content` varchar(500) DEFAULT NULL COMMENT '操作内容/备注',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作日志表';

-- ----------------------------
-- 菜单权限数据
-- ----------------------------
-- 工单管理菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('工单管理', '', 1, 80, 0, '/ticket', 'ep:tickets', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @ticket_menu_id = LAST_INSERT_ID();

-- 工单列表
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('工单列表', '', 2, 1, @ticket_menu_id, 'list', 'ep:list', 'ticket/list/index', 'TicketList', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @ticket_list_menu_id = LAST_INSERT_ID();

-- 工单按钮权限
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('工单查询', 'ticket:ticket:query', 3, 1, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单创建', 'ticket:ticket:create', 3, 2, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单更新', 'ticket:ticket:update', 3, 3, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单删除', 'ticket:ticket:delete', 3, 4, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单分配', 'ticket:ticket:assign', 3, 5, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单处理', 'ticket:ticket:process', 3, 6, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单关闭', 'ticket:ticket:close', 3, 7, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工单取消', 'ticket:ticket:cancel', 3, 8, @ticket_list_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 工单分类
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('工单分类', '', 2, 2, @ticket_menu_id, 'category', 'ep:folder', 'ticket/category/index', 'TicketCategory', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

SET @ticket_category_menu_id = LAST_INSERT_ID();

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES 
('分类查询', 'ticket:category:query', 3, 1, @ticket_category_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('分类创建', 'ticket:category:create', 3, 2, @ticket_category_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('分类更新', 'ticket:category:update', 3, 3, @ticket_category_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('分类删除', 'ticket:category:delete', 3, 4, @ticket_category_menu_id, '', '', '', '', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');
