-- 仪表板配置表
-- 用于存储用户自定义的仪表板布局配置

CREATE TABLE IF NOT EXISTS `system_dashboard_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `page_type` VARCHAR(32) NOT NULL COMMENT '页面类型: workspace-工作台, analytics-分析页',
    `layout_config` TEXT COMMENT '布局配置JSON',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_page_type` (`user_id`, `page_type`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪表板配置表';
