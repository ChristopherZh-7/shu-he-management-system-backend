-- =====================================================
-- 项目可见性闭环修复
-- 1. 历史商机：dept_allocations -> involved_dept_ids
-- 2. 历史项目：business.involved_dept_ids -> project.involved_dept_ids
-- 3. 项目可见关系：project.involved_dept_ids -> project_dept_visibility
--
-- 先补齐空值，再以部门服务单为主数据校正项目快照和可见关系；可幂等重复执行。
-- =====================================================

UPDATE crm_business AS business
JOIN (
    SELECT source.business_id, JSON_ARRAYAGG(source.dept_id) AS dept_ids
    FROM (
        SELECT DISTINCT business_row.id AS business_id, allocation.dept_id
        FROM crm_business AS business_row
        JOIN JSON_TABLE(
            COALESCE(business_row.dept_allocations, JSON_ARRAY()),
            '$[*]' COLUMNS (
                dept_id BIGINT PATH '$.deptId' NULL ON EMPTY NULL ON ERROR
            )
        ) AS allocation ON TRUE
        WHERE business_row.deleted = b'0'
          AND allocation.dept_id IS NOT NULL
    ) AS source
    GROUP BY source.business_id
) AS backfill ON backfill.business_id = business.id
SET business.involved_dept_ids = backfill.dept_ids
WHERE business.involved_dept_ids IS NULL
   OR JSON_LENGTH(business.involved_dept_ids) = 0;

UPDATE project AS project_row
JOIN crm_business AS business ON business.id = project_row.business_id
SET project_row.involved_dept_ids = business.involved_dept_ids
WHERE project_row.deleted = b'0'
  AND business.deleted = b'0'
  AND business.involved_dept_ids IS NOT NULL
  AND JSON_LENGTH(business.involved_dept_ids) > 0
  AND (
      project_row.involved_dept_ids IS NULL
      OR JSON_LENGTH(project_row.involved_dept_ids) = 0
  );

INSERT INTO project_dept_visibility (
    project_id,
    dept_id,
    creator,
    create_time,
    updater,
    update_time,
    deleted,
    tenant_id
)
SELECT DISTINCT
    project_row.id,
    involved.dept_id,
    'visibility-migration',
    NOW(),
    'visibility-migration',
    NOW(),
    b'0',
    project_row.tenant_id
FROM project AS project_row
JOIN JSON_TABLE(
    COALESCE(project_row.involved_dept_ids, JSON_ARRAY()),
    '$[*]' COLUMNS (
        dept_id BIGINT PATH '$' NULL ON EMPTY NULL ON ERROR
    )
) AS involved ON TRUE
LEFT JOIN project_dept_visibility AS existing
       ON existing.project_id = project_row.id
      AND existing.dept_id = involved.dept_id
      AND existing.deleted = b'0'
WHERE project_row.deleted = b'0'
  AND involved.dept_id IS NOT NULL
  AND existing.id IS NULL;

SELECT
    COUNT(*) AS project_total,
    SUM(involved_dept_ids IS NOT NULL AND JSON_LENGTH(involved_dept_ids) > 0) AS projects_with_involved_depts
FROM project
WHERE deleted = b'0';

SELECT
    COUNT(DISTINCT project_id) AS projects_with_visibility,
    COUNT(*) AS visibility_rows
FROM project_dept_visibility
WHERE deleted = b'0';

-- =====================================================
-- 4. 部门服务单作为项目部门分工的唯一主线
-- =====================================================

-- 历史项目若只有可见部门，自动补齐部门服务单。
INSERT INTO project_dept_service (
    project_id, business_id, contract_id, contract_no, customer_id, customer_name,
    dept_id, dept_name, dept_type, status, progress,
    creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    project_row.id, project_row.business_id, project_row.contract_id, project_row.contract_no,
    project_row.customer_id, project_row.customer_name,
    visibility.dept_id, dept.name, dept.dept_type,
    CASE project_row.status WHEN 2 THEN 4 WHEN 1 THEN 2 ELSE 1 END,
    0, 'project-package-migration', NOW(), 'project-package-migration', NOW(),
    b'0', project_row.tenant_id
FROM project AS project_row
JOIN project_dept_visibility AS visibility
  ON visibility.project_id = project_row.id AND visibility.deleted = b'0'
JOIN system_dept AS dept
  ON dept.id = visibility.dept_id AND dept.deleted = b'0' AND dept.dept_type IS NOT NULL
LEFT JOIN project_dept_service AS existing
  ON existing.project_id = project_row.id
 AND existing.dept_type = dept.dept_type
 AND existing.deleted = b'0'
WHERE project_row.deleted = b'0'
  AND existing.id IS NULL;

-- 部门服务单一旦存在，可见关系必须与之一致。
INSERT INTO project_dept_visibility (
    project_id, dept_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT DISTINCT
    dept_service.project_id, dept_service.dept_id,
    'project-package-migration', NOW(), 'project-package-migration', NOW(),
    b'0', dept_service.tenant_id
FROM project_dept_service AS dept_service
LEFT JOIN project_dept_visibility AS existing
  ON existing.project_id = dept_service.project_id
 AND existing.dept_id = dept_service.dept_id
 AND existing.deleted = b'0'
WHERE dept_service.deleted = b'0'
  AND dept_service.dept_id IS NOT NULL
  AND existing.id IS NULL;

-- 项目 JSON 只作为表单快照，从部门服务单反向校正。
UPDATE project AS project_row
JOIN (
    SELECT source.project_id, JSON_ARRAYAGG(source.dept_id) AS dept_ids
    FROM (
        SELECT DISTINCT project_id, dept_id
        FROM project_dept_service
        WHERE deleted = b'0' AND dept_id IS NOT NULL
        ORDER BY project_id, dept_id
    ) AS source
    GROUP BY source.project_id
) AS packages ON packages.project_id = project_row.id
SET project_row.involved_dept_ids = packages.dept_ids
WHERE project_row.deleted = b'0';

-- 服务项必须同时归属部门服务单和其负责部门。
UPDATE project_info AS service_item
JOIN project_dept_service AS dept_service
  ON dept_service.project_id = service_item.project_id
 AND dept_service.dept_type = service_item.dept_type
 AND dept_service.deleted = b'0'
SET service_item.dept_service_id = dept_service.id,
    service_item.dept_id = COALESCE(service_item.dept_id, dept_service.dept_id),
    service_item.updater = 'project-package-migration',
    service_item.update_time = NOW()
WHERE service_item.deleted = b'0'
  AND (service_item.dept_service_id IS NULL OR service_item.dept_id IS NULL);

-- 用实际服务项刷新部门服务包进度。
UPDATE project_dept_service AS dept_service
JOIN (
    SELECT dept_service_id,
           ROUND(AVG(COALESCE(progress, 0))) AS calculated_progress,
           COUNT(*) AS total_count,
           SUM(status = 3) AS completed_count,
           SUM(status = 1) AS active_count
    FROM project_info
    WHERE deleted = b'0' AND visible = 1 AND dept_service_id IS NOT NULL
    GROUP BY dept_service_id
) AS item_stats ON item_stats.dept_service_id = dept_service.id
SET dept_service.progress = item_stats.calculated_progress,
    dept_service.status = CASE
        WHEN item_stats.completed_count = item_stats.total_count THEN 4
        WHEN item_stats.active_count > 0 THEN 2
        ELSE dept_service.status
    END,
    dept_service.updater = 'project-package-migration',
    dept_service.update_time = NOW()
WHERE dept_service.deleted = b'0';

-- 删除没有驻场服务、没有人员、没有地址联系信息的历史默认空站点。
UPDATE project_site AS site
SET site.deleted = b'1',
    site.updater = 'project-package-migration',
    site.update_time = NOW()
WHERE site.deleted = b'0'
  AND site.name = '默认管理站点'
  AND COALESCE(site.address, '') = ''
  AND COALESCE(site.contact_name, '') = ''
  AND COALESCE(site.contact_phone, '') = ''
  AND COALESCE(site.staff_count, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM project_site_member AS member
      WHERE member.site_id = site.id AND member.deleted = b'0'
  )
  AND NOT EXISTS (
      SELECT 1 FROM project_info AS service_item
      WHERE service_item.project_id = site.project_id
        AND service_item.dept_type = site.dept_type
        AND service_item.deleted = b'0'
        AND service_item.visible = 1
        AND (
            service_item.service_mode = 1
            OR service_item.service_member_type = 1
        )
  );

-- =====================================================
-- 5. 参与部门普通员工获得项目管理只读入口
--    数据边界仍由 ProjectAccessService 按项目参与部门判定。
-- =====================================================
INSERT INTO system_role_menu (
    role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT role.id, menu.id,
       'project-readonly-migration', NOW(), 'project-readonly-migration', NOW(),
       b'0', role.tenant_id
FROM system_role AS role
JOIN system_menu AS menu
  ON menu.deleted = b'0'
 AND (
      menu.id = 5080
      OR menu.permission IN (
          'project:project:query',
          'project:dept-service:query',
          'project:service-item:query',
          'project:site:query'
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
    project_row.id,
    project_row.name,
    JSON_LENGTH(project_row.involved_dept_ids) AS involved_dept_count,
    COUNT(DISTINCT dept_service.id) AS dept_service_count,
    COUNT(DISTINCT service_item.id) AS service_item_count
FROM project AS project_row
LEFT JOIN project_dept_service AS dept_service
  ON dept_service.project_id = project_row.id AND dept_service.deleted = b'0'
LEFT JOIN project_info AS service_item
  ON service_item.project_id = project_row.id AND service_item.deleted = b'0'
WHERE project_row.deleted = b'0'
GROUP BY project_row.id, project_row.name, project_row.involved_dept_ids
ORDER BY project_row.id;
