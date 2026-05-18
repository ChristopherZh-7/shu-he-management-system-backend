-- =====================================================
-- 工单中心模块 - 数据表
-- 设计原则：作为跨业务的统一入口和聚合层
--   - 工单主表 + 分类 + 操作日志 + 评论 + 附件
--   - 通过 business_type + business_id 关联到现有业务表（outside_request 等）
--   - dept_id + assignee_id 对齐数据权限规则
-- 注意：与历史的 ticket / ticket_log / ticket_category 表不冲突（新表加 shuhe_ 前缀）
-- =====================================================

-- 1. 工单主表
CREATE TABLE IF NOT EXISTS `shuhe_ticket` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_no`           VARCHAR(32)  NOT NULL                COMMENT '工单编号，如 TK20260518001',
    `title`               VARCHAR(200) NOT NULL                COMMENT '工单标题',
    `content`             TEXT         DEFAULT NULL            COMMENT '工单描述（富文本）',
    `category_id`         BIGINT       DEFAULT NULL            COMMENT '工单分类ID（关联 shuhe_ticket_category）',
    `priority`            TINYINT      NOT NULL DEFAULT 1      COMMENT '优先级：0低 1中 2高 3紧急',
    `source`              TINYINT      NOT NULL DEFAULT 0      COMMENT '来源：0手动 1外协 2服务派遣 3API',
    `business_type`       VARCHAR(32)  NOT NULL DEFAULT 'general' COMMENT '业务类型：general/outside_request/service_launch/...',
    `business_id`         BIGINT       DEFAULT NULL            COMMENT '关联业务表ID',
    `process_instance_id` VARCHAR(64)  DEFAULT NULL            COMMENT '关联 BPM 流程实例ID',
    `status`              TINYINT      NOT NULL DEFAULT 0      COMMENT '0待处理 1处理中 2待审核 3已完成 4已关闭 5已取消',
    `sub_status`          VARCHAR(32)  DEFAULT NULL            COMMENT '子状态',
    `creator_id`          BIGINT       NOT NULL                COMMENT '提单人ID',
    `creator_name`        VARCHAR(64)  DEFAULT NULL            COMMENT '提单人姓名快照',
    `assignee_id`         BIGINT       DEFAULT NULL            COMMENT '当前处理人ID（数据权限 user_id 字段）',
    `assignee_name`       VARCHAR(64)  DEFAULT NULL            COMMENT '处理人姓名快照',
    `assignee_dept_id`    BIGINT       DEFAULT NULL            COMMENT '处理人部门ID',
    `dept_id`             BIGINT       NOT NULL                COMMENT '工单归属部门ID（数据权限字段）',
    `due_time`            DATETIME     DEFAULT NULL            COMMENT '截止时间（SLA）',
    `first_response_time` DATETIME     DEFAULT NULL            COMMENT '首次响应时间',
    `finish_time`         DATETIME     DEFAULT NULL            COMMENT '完成时间',
    `close_time`          DATETIME     DEFAULT NULL            COMMENT '关闭时间',
    `notify_channels`     VARCHAR(64)  DEFAULT 'inner,dingtalk' COMMENT '通知通道：inner/dingtalk/sms/email',
    `notify_status`       TINYINT      DEFAULT 0               COMMENT '0未通知 1已通知 2失败',
    `project_id`          BIGINT       DEFAULT NULL            COMMENT '关联项目ID（可选）',
    `customer_id`         BIGINT       DEFAULT NULL            COMMENT '关联客户ID（可选）',
    `ext_json`            JSON         DEFAULT NULL            COMMENT '扩展字段',
    `remark`              VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    `creator`             VARCHAR(64)  DEFAULT '',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`             VARCHAR(64)  DEFAULT '',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             BIT(1)       NOT NULL DEFAULT b'0',
    `tenant_id`           BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_creator_id`  (`creator_id`),
    KEY `idx_assignee_id` (`assignee_id`),
    KEY `idx_dept_id`     (`dept_id`),
    KEY `idx_status`      (`status`),
    KEY `idx_business`    (`business_type`, `business_id`),
    KEY `idx_project_id`  (`project_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单主表';

-- 2. 工单分类（树形）
CREATE TABLE IF NOT EXISTS `shuhe_ticket_category` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`                BIGINT       NOT NULL DEFAULT 0  COMMENT '父分类ID，0=顶级',
    `name`                     VARCHAR(100) NOT NULL            COMMENT '分类名称',
    `code`                     VARCHAR(50)  DEFAULT NULL        COMMENT '分类编码',
    `icon`                     VARCHAR(100) DEFAULT NULL        COMMENT '图标',
    `sort`                     INT          NOT NULL DEFAULT 0,
    `default_assignee_id`      BIGINT       DEFAULT NULL        COMMENT '默认处理人',
    `default_assignee_dept_id` BIGINT       DEFAULT NULL        COMMENT '默认处理部门',
    `default_priority`         TINYINT      DEFAULT 1,
    `default_sla_hours`        INT          DEFAULT NULL        COMMENT '默认 SLA 小时',
    `status`                   TINYINT      NOT NULL DEFAULT 0  COMMENT '0启用 1禁用',
    `creator`                  VARCHAR(64)  DEFAULT '',
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`                  VARCHAR(64)  DEFAULT '',
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  BIT(1)       NOT NULL DEFAULT b'0',
    `tenant_id`                BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单分类';

-- 3. 工单操作日志
CREATE TABLE IF NOT EXISTS `shuhe_ticket_log` (
    `id`               BIGINT      NOT NULL AUTO_INCREMENT,
    `ticket_id`        BIGINT      NOT NULL,
    `operator_id`      BIGINT      NOT NULL,
    `operator_name`    VARCHAR(64) DEFAULT NULL,
    `action`           VARCHAR(32) NOT NULL  COMMENT 'create/assign/start/comment/finish/close/reopen/transfer/cancel',
    `from_status`      TINYINT     DEFAULT NULL,
    `to_status`        TINYINT     DEFAULT NULL,
    `from_assignee_id` BIGINT      DEFAULT NULL,
    `to_assignee_id`   BIGINT      DEFAULT NULL,
    `content`          VARCHAR(1000) DEFAULT NULL  COMMENT '操作说明/备注',
    `ext_json`         JSON          DEFAULT NULL,
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`          VARCHAR(64) DEFAULT '',
    `tenant_id`        BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id`   (`ticket_id`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作日志';

-- 4. 工单评论
CREATE TABLE IF NOT EXISTS `shuhe_ticket_comment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `ticket_id`     BIGINT       NOT NULL,
    `user_id`       BIGINT       NOT NULL,
    `user_name`     VARCHAR(64)  DEFAULT NULL,
    `user_dept_id`  BIGINT       DEFAULT NULL,
    `parent_id`     BIGINT       DEFAULT NULL  COMMENT '@回复用',
    `content`       TEXT         NOT NULL,
    `is_internal`   BIT(1)       NOT NULL DEFAULT b'0'  COMMENT '是否内部评论（提单人看不到）',
    `creator`       VARCHAR(64)  DEFAULT '',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`       VARCHAR(64)  DEFAULT '',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       BIT(1)       NOT NULL DEFAULT b'0',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单评论';

-- 5. 工单附件
CREATE TABLE IF NOT EXISTS `shuhe_ticket_attachment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `ticket_id`     BIGINT       NOT NULL,
    `comment_id`    BIGINT       DEFAULT NULL  COMMENT '关联评论ID（评论附件）',
    `file_name`     VARCHAR(255) NOT NULL,
    `file_url`      VARCHAR(500) NOT NULL,
    `file_size`     BIGINT       DEFAULT NULL,
    `file_type`     VARCHAR(64)  DEFAULT NULL  COMMENT 'MIME 类型',
    `uploader_id`   BIGINT       NOT NULL,
    `creator`       VARCHAR(64)  DEFAULT '',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater`       VARCHAR(64)  DEFAULT '',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       BIT(1)       NOT NULL DEFAULT b'0',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单附件';
