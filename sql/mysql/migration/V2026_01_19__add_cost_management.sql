-- 为部门表添加部门类型字段
ALTER TABLE system_dept ADD COLUMN IF NOT EXISTS dept_type TINYINT DEFAULT NULL COMMENT '部门类型：1安全服务 2安全运营 3数据安全';

-- 创建节假日缓存表
CREATE TABLE IF NOT EXISTS system_holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    date DATE NOT NULL COMMENT '日期',
    year INT NOT NULL COMMENT '年份',
    month INT NOT NULL COMMENT '月份',
    is_holiday TINYINT NOT NULL DEFAULT 0 COMMENT '是否节假日：1是 0否',
    holiday_name VARCHAR(50) DEFAULT NULL COMMENT '节假日名称',
    is_workday TINYINT NOT NULL DEFAULT 1 COMMENT '是否工作日（含调休）：1是 0否',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    UNIQUE KEY uk_date (date, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节假日缓存表';

-- 添加成本管理菜单
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('成本管理', '', 2, 15, 1, 'cost', 'ep:money', 'system/cost/index', 'SystemCost', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 获取刚插入的菜单ID
SET @cost_menu_id = LAST_INSERT_ID();

-- 添加成本管理的权限按钮
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES ('成本查询', 'system:cost:query', 3, 1, @cost_menu_id, '', '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 为超级管理员角色分配成本管理权限
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, @cost_menu_id, '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = @cost_menu_id);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 1, LAST_INSERT_ID(), '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (SELECT 1 FROM system_role_menu WHERE role_id = 1 AND menu_id = LAST_INSERT_ID());
