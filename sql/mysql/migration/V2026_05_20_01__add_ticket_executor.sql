-- =====================================================
-- 工单中心模块 - 多执行人扩展
-- 场景：主管「接单」时可指定 1+ 个执行人；接单后自动触发业务驱动器（service_launch 等）
-- 设计原则：
--   - 与 shuhe_ticket.assignee_id（处理人/主管）分离；这里只记录被指派的实际执行人
--   - 唯一索引 uk_ticket_user 防同一执行人重复添加；deleted 入键保证逻辑删除不冲突
--   - 关键索引覆盖「按 ticket 查所有执行人」「按 user 查我执行的工单」两条主查询
-- =====================================================

CREATE TABLE IF NOT EXISTS `shuhe_ticket_executor` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`    BIGINT       NOT NULL                COMMENT '工单 ID（关联 shuhe_ticket.id）',
    `user_id`      BIGINT       NOT NULL                COMMENT '执行人用户 ID',
    `user_name`    VARCHAR(64)  DEFAULT NULL            COMMENT '执行人姓名快照',
    `user_dept_id` BIGINT       DEFAULT NULL            COMMENT '执行人部门 ID 快照',
    `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '0执行中 1已完成 2已退出',
    `assigned_by`  BIGINT       NOT NULL                COMMENT '分派人（接单的主管）ID',
    `remark`       VARCHAR(500) DEFAULT NULL            COMMENT '分派备注',
    `creator`      VARCHAR(64)  DEFAULT '',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`      VARCHAR(64)  DEFAULT '',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      BIT(1)       NOT NULL DEFAULT b'0',
    `tenant_id`    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_user` (`ticket_id`, `user_id`, `deleted`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_user_id`   (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单执行人（多执行人关联表）';

-- 接单权限（按钮级）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `status`
)
SELECT '工单接单', 'ticket:ticket:accept', 3, 8,
       (SELECT id FROM (SELECT id FROM `system_menu` WHERE `permission` = 'ticket:ticket:query' LIMIT 1) t),
       '', '', '', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'ticket:ticket:accept');

-- 把接单权限授予 super_admin（role_id=1）
INSERT INTO `system_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.id FROM `system_menu` m
WHERE m.`permission` = 'ticket:ticket:accept'
  AND NOT EXISTS (
      SELECT 1 FROM `system_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
