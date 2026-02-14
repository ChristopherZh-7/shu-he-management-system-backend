-- =============================================
-- 重构工作台菜单结构
--
-- 变更内容：
-- 1. system_users 表增加 work_mode 字段（1-驻场, 2-二线）
-- 2. 创建 project_report 表（项目周报/汇报）
-- 3. 新增三个工作台子菜单：我的工作记录、团队工作总览、项目周报/汇报
-- 4. 为各角色分配新菜单权限
-- 5. 保留旧菜单（向后兼容）
-- =============================================

SET NAMES utf8mb4;

-- =============================================
-- 第一步：用户表增加 work_mode 字段
-- =============================================

SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_users'
      AND COLUMN_NAME = 'work_mode'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `system_users` ADD COLUMN `work_mode` tinyint NULL DEFAULT 2 COMMENT ''工作模式 1-驻场 2-二线''',
    'SELECT ''work_mode column already exists'' AS info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 部门表增加 work_mode 字段（null=继承上级, 1=驻场, 2=二线）
SET @dept_col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'system_dept'
      AND COLUMN_NAME = 'work_mode'
);

SET @dept_sql = IF(@dept_col_exists = 0,
    'ALTER TABLE `system_dept` ADD COLUMN `work_mode` tinyint NULL DEFAULT NULL COMMENT ''工作模式 null-继承上级 1-驻场 2-二线''',
    'SELECT ''system_dept.work_mode column already exists'' AS info'
);

PREPARE dept_stmt FROM @dept_sql;
EXECUTE dept_stmt;
DEALLOCATE PREPARE dept_stmt;

-- =============================================
-- 第二步：创建 project_report 表
-- =============================================

CREATE TABLE IF NOT EXISTS `project_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `project_type` tinyint NULL DEFAULT NULL COMMENT '项目类型 1-安全服务 2-安全运营 3-数据安全',
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '项目名称',
  `year` int NOT NULL COMMENT '年份',
  `week_number` int NOT NULL COMMENT '周数（1-53）',
  `week_start_date` date NULL DEFAULT NULL COMMENT '本周开始日期（周一）',
  `week_end_date` date NULL DEFAULT NULL COMMENT '本周结束日期（周五）',
  `progress` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '项目进展',
  `issues` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '遇到的问题',
  `resources` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '需要的资源/协调事项',
  `risks` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '风险提示',
  `next_week_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '下周计划',
  `attachments` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附件URL列表（JSON数组）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '记录人姓名',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '记录人部门ID',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部门名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_project_id` (`project_id`) USING BTREE,
  INDEX `idx_creator` (`creator`) USING BTREE,
  INDEX `idx_year_week` (`year`, `week_number`) USING BTREE,
  UNIQUE INDEX `uk_creator_project_year_week` (`creator`, `project_id`, `year`, `week_number`, `deleted`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '项目周报/汇报';

-- =============================================
-- 第三步：获取工作台菜单ID
-- =============================================

SET @workbench_id = (
    SELECT `id` FROM `system_menu`
    WHERE (`path` = 'workbench' OR `name` = '工作台')
      AND `deleted` = 0
    LIMIT 1
);

SELECT CONCAT('工作台菜单ID: ', IFNULL(@workbench_id, '未找到')) AS debug_info;

-- =============================================
-- 第四步：创建新菜单 - 我的工作记录
-- =============================================

SET @my_work_id = (
    SELECT `id` FROM `system_menu`
    WHERE `path` = 'my-work-record' AND `deleted` = 0
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`,
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '我的工作记录', '', 2, 0, @workbench_id, 'my-work-record',
       'ep:edit', 'dashboard/my-work-record/index', 'WorkbenchMyWorkRecord',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @my_work_id IS NULL AND @workbench_id IS NOT NULL;

SET @my_work_id = COALESCE(
    @my_work_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'my-work-record' AND `deleted` = 0 LIMIT 1)
);

-- 我的工作记录权限按钮（复用已有权限标识）
SET @mw_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:query' AND `deleted` = 0 LIMIT 1);
SET @mw_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:create' AND `deleted` = 0 LIMIT 1);
SET @mw_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:update' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的工作记录查询', 'project:my-work-record:query', 3, 1, @my_work_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mw_query_id IS NULL AND @my_work_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的工作记录创建', 'project:my-work-record:create', 3, 2, @my_work_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mw_create_id IS NULL AND @my_work_id IS NOT NULL;

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '我的工作记录修改', 'project:my-work-record:update', 3, 3, @my_work_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @mw_update_id IS NULL AND @my_work_id IS NOT NULL;

SET @mw_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:query' AND `deleted` = 0 LIMIT 1);
SET @mw_create_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:create' AND `deleted` = 0 LIMIT 1);
SET @mw_update_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:my-work-record:update' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第五步：创建新菜单 - 团队工作总览
-- =============================================

SET @team_overview_id = (
    SELECT `id` FROM `system_menu`
    WHERE `path` = 'team-overview' AND `deleted` = 0
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
    `component`, `component_name`, `status`, `visible`, `keep_alive`,
    `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '团队工作总览', '', 2, 1, @workbench_id, 'team-overview',
       'ep:user', 'dashboard/team-overview/index', 'WorkbenchTeamOverview',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @team_overview_id IS NULL AND @workbench_id IS NOT NULL;

SET @team_overview_id = COALESCE(
    @team_overview_id,
    (SELECT `id` FROM `system_menu` WHERE `path` = 'team-overview' AND `deleted` = 0 LIMIT 1)
);

-- 团队工作总览权限按钮
SET @to_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:team-overview:query' AND `deleted` = 0 LIMIT 1);

INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '团队总览查询', 'project:team-overview:query', 3, 1, @team_overview_id, '', '', '', 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL WHERE @to_query_id IS NULL AND @team_overview_id IS NOT NULL;

SET @to_query_id = (SELECT `id` FROM `system_menu` WHERE `permission` = 'project:team-overview:query' AND `deleted` = 0 LIMIT 1);

-- =============================================
-- 第六步：隐藏旧菜单（保留数据不删除）
-- =============================================

-- 将旧的三个菜单隐藏（visible = false），但不删除
UPDATE `system_menu`
SET `visible` = b'0', `sort` = `sort` + 100, `update_time` = NOW()
WHERE `path` IN ('weekly-work-calendar', 'project-management-record', 'daily-management-record')
  AND `deleted` = 0;

-- =============================================
-- 第八步：为各角色分配新菜单权限
-- =============================================

-- 超级管理员：所有权限
SET @admin_role_id = 1;

INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT @admin_role_id, id, '1', NOW(), '1', NOW(), b'0'
FROM `system_menu`
WHERE `id` IN (
    @my_work_id, @mw_query_id, @mw_create_id, @mw_update_id,
    @team_overview_id, @to_query_id
) AND `deleted` = 0;

-- 主管角色：我的工作记录 + 团队工作总览
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_mg', 'ay_mg', 'sh_mg')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @my_work_id, @mw_query_id, @mw_create_id, @mw_update_id,
    @team_overview_id, @to_query_id
  );

-- 组长角色：我的工作记录 + 团队工作总览
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_tl', 'ay_tl', 'sh_tl')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @my_work_id, @mw_query_id, @mw_create_id, @mw_update_id,
    @team_overview_id, @to_query_id
  );

-- 工程师角色：仅我的工作记录（无团队总览、无项目周报）
INSERT IGNORE INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT r.id, m.id, '1', NOW(), '1', NOW(), 0, 1
FROM `system_role` r, `system_menu` m
WHERE r.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND r.deleted = 0
  AND m.deleted = 0
  AND m.id IN (
    @my_work_id, @mw_query_id, @mw_create_id, @mw_update_id
  );

-- =============================================
-- 第九步：验证
-- =============================================

SELECT '===== 新工作台菜单结构 =====' AS title;

SELECT
    m.id,
    m.name,
    m.path,
    m.permission,
    m.component,
    m.component_name,
    m.parent_id,
    m.sort,
    CASE m.type
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单'
        WHEN 3 THEN '按钮'
    END AS menu_type,
    CASE WHEN m.visible = b'1' THEN '可见' ELSE '隐藏' END AS visibility
FROM `system_menu` m
WHERE (
    m.id = @workbench_id
    OR m.parent_id = @workbench_id
    OR m.parent_id IN (
        SELECT id FROM `system_menu` WHERE parent_id = @workbench_id AND deleted = 0
    )
)
AND m.deleted = 0
ORDER BY
    CASE WHEN m.id = @workbench_id THEN 0 ELSE 1 END,
    m.sort, m.id;

SELECT '✅ 工作台菜单重构完成！' AS result;
