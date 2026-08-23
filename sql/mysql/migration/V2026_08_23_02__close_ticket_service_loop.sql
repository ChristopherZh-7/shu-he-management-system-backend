-- =====================================================
-- 服务工单闭环
-- 1. 工单精确关联 service_item_id，消除同类型服务项串单
-- 2. 历史 service_launch 工单回填项目/服务项/负责部门快照
-- 3. 工程师角色可申请、执行、验收自己相关工单
-- 4. 工单菜单收口为单一“服务工单”入口，清理乱码重复路由
-- =====================================================

-- MySQL 无 ALTER TABLE ADD COLUMN IF NOT EXISTS，使用 information_schema 保持幂等。
SET @ticket_service_item_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'shuhe_ticket'
      AND COLUMN_NAME = 'service_item_id'
);
SET @ticket_service_item_column_sql = IF(
    @ticket_service_item_column_exists = 0,
    'ALTER TABLE shuhe_ticket ADD COLUMN service_item_id BIGINT DEFAULT NULL COMMENT ''精确服务项ID'' AFTER project_id, ADD INDEX idx_service_item_id (service_item_id)',
    'SELECT 1'
);
PREPARE ticket_service_item_column_stmt FROM @ticket_service_item_column_sql;
EXECUTE ticket_service_item_column_stmt;
DEALLOCATE PREPARE ticket_service_item_column_stmt;

-- 已生成执行记录的历史工单可精确回填。
UPDATE shuhe_ticket AS ticket
JOIN project_service_launch AS launch
  ON launch.id = ticket.business_id
 AND launch.deleted = b'0'
SET ticket.service_item_id = launch.service_item_id,
    ticket.updater = 'ticket-service-migration',
    ticket.update_time = NOW()
WHERE ticket.deleted = b'0'
  AND ticket.business_type = 'service_launch'
  AND ticket.service_item_id IS NULL
  AND launch.service_item_id IS NOT NULL;

-- 兼容已经在 ext_json 中保存过 serviceItemId 的数据。
UPDATE shuhe_ticket
SET service_item_id = CAST(JSON_UNQUOTE(JSON_EXTRACT(ext_json, '$.serviceItemId')) AS UNSIGNED),
    updater = 'ticket-service-migration',
    update_time = NOW()
WHERE deleted = b'0'
  AND service_item_id IS NULL
  AND JSON_EXTRACT(ext_json, '$.serviceItemId') IS NOT NULL;

-- 用服务项主数据统一项目、客户、负责部门与展示快照。
UPDATE shuhe_ticket AS ticket
JOIN project_info AS item
  ON item.id = ticket.service_item_id
 AND item.deleted = b'0'
JOIN project AS project_row
  ON project_row.id = item.project_id
 AND project_row.deleted = b'0'
LEFT JOIN project_dept_service AS dept_service
  ON dept_service.id = item.dept_service_id
 AND dept_service.deleted = b'0'
LEFT JOIN system_dept AS responsible_dept
  ON responsible_dept.id = COALESCE(item.dept_id, dept_service.dept_id)
 AND responsible_dept.deleted = b'0'
SET ticket.project_id = item.project_id,
    ticket.customer_id = COALESCE(item.customer_id, project_row.customer_id),
    ticket.dept_id = COALESCE(item.dept_id, dept_service.dept_id, ticket.dept_id),
    ticket.ext_json = JSON_SET(
        COALESCE(ticket.ext_json, JSON_OBJECT()),
        '$.serviceItemId', item.id,
        '$.serviceItemCode', item.code,
        '$.serviceType', item.service_type,
        '$.serviceTypeName', item.service_type,
        '$.serviceMode', item.service_mode,
        '$.deptType', item.dept_type,
        '$.projectId', project_row.id,
        '$.projectCode', project_row.code,
        '$.projectName', project_row.name,
        '$.executeDeptId', COALESCE(item.dept_id, dept_service.dept_id),
        '$.responsibleDeptName', COALESCE(responsible_dept.name, dept_service.dept_name),
        '$.customerId', COALESCE(item.customer_id, project_row.customer_id),
        '$.customerName', COALESCE(item.customer_name, project_row.customer_name),
        '$.serviceSourceType', IF(COALESCE(item.contract_id, project_row.contract_id) IS NULL,
                                  'approved_early_investment', 'signed_contract')
    ),
    ticket.updater = 'ticket-service-migration',
    ticket.update_time = NOW()
WHERE ticket.deleted = b'0';

-- 收口为一个用户语言上的“服务工单”。
SET @ticket_top = (
    SELECT id FROM system_menu
    WHERE parent_id = 0 AND path = '/ticket' AND component = 'Layout' AND deleted = b'0'
    ORDER BY id LIMIT 1
);
SET @ticket_list = (
    SELECT id FROM system_menu
    WHERE parent_id = @ticket_top AND component = 'ticket/list/index' AND deleted = b'0'
    ORDER BY id LIMIT 1
);

UPDATE system_menu
SET name = '服务工单', updater = 'ticket-service-migration', update_time = NOW()
WHERE id = @ticket_top;

UPDATE system_menu
SET name = '服务工单', updater = 'ticket-service-migration', update_time = NOW()
WHERE id = @ticket_list;

-- 先删重复根的子孙菜单，再删根；不依赖乱码名称。
UPDATE system_menu AS grandchild
JOIN system_menu AS child ON child.id = grandchild.parent_id
JOIN system_menu AS duplicate_root ON duplicate_root.id = child.parent_id
SET grandchild.deleted = b'1', grandchild.updater = 'ticket-service-migration', grandchild.update_time = NOW()
WHERE duplicate_root.parent_id = 0
  AND duplicate_root.path = '/ticket'
  AND duplicate_root.id <> @ticket_top
  AND duplicate_root.deleted = b'0'
  AND grandchild.deleted = b'0';

UPDATE system_menu AS child
JOIN system_menu AS duplicate_root ON duplicate_root.id = child.parent_id
SET child.deleted = b'1', child.updater = 'ticket-service-migration', child.update_time = NOW()
WHERE duplicate_root.parent_id = 0
  AND duplicate_root.path = '/ticket'
  AND duplicate_root.id <> @ticket_top
  AND duplicate_root.deleted = b'0'
  AND child.deleted = b'0';

UPDATE system_menu
SET deleted = b'1', updater = 'ticket-service-migration', update_time = NOW()
WHERE parent_id = 0
  AND path = '/ticket'
  AND id <> @ticket_top
  AND deleted = b'0';

-- 所有工程师可进入、申请、查看以及完成自己相关的工单。
-- 接单权限 ticket:ticket:accept 仍只给部门负责人/管理角色。
INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT role.id, menu.id,
       'ticket-service-migration', NOW(), 'ticket-service-migration', NOW(),
       b'0', role.tenant_id
FROM system_role AS role
JOIN system_menu AS menu
  ON menu.deleted = b'0'
 AND (
      menu.id IN (@ticket_top, @ticket_list)
      OR (menu.parent_id = @ticket_top AND menu.component = 'ticket/detail/index')
      OR menu.permission IN (
          'ticket:ticket:create',
          'ticket:ticket:query',
          'ticket:ticket:update',
          'ticket:ticket:finish',
          'ticket:ticket:close',
          'ticket:ticket:transfer',
          'ticket:comment:create'
      )
 )
LEFT JOIN system_role_menu AS existing
  ON existing.role_id = role.id
 AND existing.menu_id = menu.id
 AND existing.deleted = b'0'
WHERE role.deleted = b'0'
  AND role.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND existing.id IS NULL;

SELECT
    COUNT(*) AS active_ticket_count,
    SUM(service_item_id IS NOT NULL) AS tickets_with_exact_service_item
FROM shuhe_ticket
WHERE deleted = b'0';

SELECT role.code, COUNT(*) AS ticket_permissions
FROM system_role AS role
JOIN system_role_menu AS role_menu
  ON role_menu.role_id = role.id AND role_menu.deleted = b'0'
JOIN system_menu AS menu
  ON menu.id = role_menu.menu_id AND menu.deleted = b'0'
WHERE role.deleted = b'0'
  AND role.code IN ('af_emp', 'ay_emp', 'sh_emp')
  AND (menu.id IN (@ticket_top, @ticket_list) OR menu.permission LIKE 'ticket:%')
GROUP BY role.code
ORDER BY role.code;
