-- =============================================================================
-- project_dept_service 补全 dept_id：先查后改
--
-- 用法：请只执行「第一部分」各段 SELECT，确认数据后再考虑执行文件末尾的 UPDATE。
-- 要求：MySQL 8.0+（JSON_TABLE）
-- =============================================================================


-- =============================================================================
-- 第一部分：只读查询（默认只执行本段，安全）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1.1 总览：dept_id 为空的部门服务单数量
-- -----------------------------------------------------------------------------
SELECT COUNT(*) AS need_fill_count
FROM project_dept_service
WHERE deleted = 0
  AND dept_id IS NULL;

-- 按部门类型分布
SELECT dept_type,
       CASE dept_type
           WHEN 1 THEN '安全服务'
           WHEN 2 THEN '安全运营'
           WHEN 3 THEN '数据安全'
           ELSE CAST(dept_type AS CHAR)
           END AS dept_type_label,
       COUNT(*) AS cnt
FROM project_dept_service
WHERE deleted = 0
  AND dept_id IS NULL
GROUP BY dept_type
ORDER BY dept_type;


-- -----------------------------------------------------------------------------
-- 1.2 明细：所有 dept_id 为空的部门服务单（带项目编号、商机解析方式）
-- -----------------------------------------------------------------------------
SELECT pds.id AS dept_service_id,
       pds.project_id,
       pr.code AS project_code,
       pr.name AS project_name,
       pds.business_id AS pds_business_id,
       pr.business_id AS project_business_id,
       COALESCE(pds.business_id, pr.business_id) AS resolved_business_id,
       pds.dept_type,
       CASE pds.dept_type
           WHEN 1 THEN '安全服务'
           WHEN 2 THEN '安全运营'
           WHEN 3 THEN '数据安全'
           ELSE CAST(pds.dept_type AS CHAR)
           END AS dept_type_label,
       pds.claimed,
       CASE
           WHEN COALESCE(pds.business_id, pr.business_id) IS NULL THEN '无商机ID'
           WHEN b.id IS NULL THEN '商机不存在'
           WHEN b.dept_allocations IS NULL OR JSON_LENGTH(b.dept_allocations) = 0 THEN '商机无部门分配'
           ELSE CONCAT('JSON首元素类型=', JSON_TYPE(JSON_EXTRACT(b.dept_allocations, '$[0]')))
           END AS business_alloc_hint
FROM project_dept_service pds
         INNER JOIN project pr ON pr.id = pds.project_id AND pr.deleted = 0
         LEFT JOIN crm_business b ON b.id = COALESCE(pds.business_id, pr.business_id) AND b.deleted = 0
WHERE pds.deleted = 0
  AND pds.dept_id IS NULL
ORDER BY pds.id;


-- -----------------------------------------------------------------------------
-- 1.3 可自动匹配（新格式 JSON：首元素为 OBJECT）— 将写入的 dept_id / dept_name 预览
--     与下文「UPDATE 新格式」逻辑一致（每行一条部门服务单）
-- -----------------------------------------------------------------------------
SELECT pds2.id AS dept_service_id,
       p2.code AS project_code,
       MIN(j.alloc_dept_id) AS would_set_dept_id,
       ANY_VALUE(COALESCE(NULLIF(j.alloc_dept_name, ''), sd.name)) AS would_set_dept_name
FROM project_dept_service pds2
         INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
         INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
         INNER JOIN JSON_TABLE(
        b2.dept_allocations,
        '$[*]' COLUMNS(
            alloc_dept_id BIGINT PATH '$.deptId',
            alloc_dept_name VARCHAR(256) PATH '$.deptName'
            )
                    ) AS j
         INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
WHERE pds2.deleted = 0
  AND pds2.dept_id IS NULL
  AND b2.dept_allocations IS NOT NULL
  AND JSON_LENGTH(b2.dept_allocations) > 0
  AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) = 'OBJECT'
  AND sd.dept_type = pds2.dept_type
GROUP BY pds2.id, p2.code;


-- -----------------------------------------------------------------------------
-- 1.4 可自动匹配（旧格式 JSON：纯数字数组）— 预览将写入的值（与「UPDATE 旧格式」一致）
-- -----------------------------------------------------------------------------
SELECT pds2.id AS dept_service_id,
       p2.code AS project_code,
       MIN(j.alloc_dept_id) AS would_set_dept_id,
       ANY_VALUE(sd.name) AS would_set_dept_name
FROM project_dept_service pds2
         INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
         INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
         INNER JOIN JSON_TABLE(
        b2.dept_allocations,
        '$[*]' COLUMNS(
            alloc_dept_id BIGINT PATH '$'
            )
                    ) AS j
         INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
WHERE pds2.deleted = 0
  AND pds2.dept_id IS NULL
  AND b2.dept_allocations IS NOT NULL
  AND JSON_LENGTH(b2.dept_allocations) > 0
  AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) IN ('INTEGER', 'DOUBLE')
  AND sd.dept_type = pds2.dept_type
GROUP BY pds2.id, p2.code;


-- -----------------------------------------------------------------------------
-- 1.5 仍无法自动匹配的行（dept_id 为空，且既不符合新格式也不符合旧格式的可解析匹配）
--     需人工核对商机分配或手工 UPDATE
-- -----------------------------------------------------------------------------
SELECT pds.id AS dept_service_id,
       pr.code AS project_code,
       COALESCE(pds.business_id, pr.business_id) AS resolved_business_id,
       pds.dept_type,
       CASE
           WHEN COALESCE(pds.business_id, pr.business_id) IS NULL THEN '缺少商机ID'
           WHEN b.id IS NULL THEN '商机不存在'
           WHEN b.dept_allocations IS NULL OR JSON_LENGTH(b.dept_allocations) = 0 THEN '商机部门分配为空'
           WHEN JSON_TYPE(JSON_EXTRACT(b.dept_allocations, '$[0]')) NOT IN ('OBJECT', 'INTEGER', 'DOUBLE')
               THEN CONCAT('不支持的JSON首类型:', JSON_TYPE(JSON_EXTRACT(b.dept_allocations, '$[0]')))
           ELSE '部门分配中无与 dept_type 一致的 system_dept'
           END AS reason
FROM project_dept_service pds
         INNER JOIN project pr ON pr.id = pds.project_id AND pr.deleted = 0
         LEFT JOIN crm_business b ON b.id = COALESCE(pds.business_id, pr.business_id) AND b.deleted = 0
WHERE pds.deleted = 0
  AND pds.dept_id IS NULL
  AND pds.id NOT IN (
    SELECT pds2.id
    FROM project_dept_service pds2
             INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
             INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
             INNER JOIN JSON_TABLE(
            b2.dept_allocations,
            '$[*]' COLUMNS(
                alloc_dept_id BIGINT PATH '$.deptId',
                alloc_dept_name VARCHAR(256) PATH '$.deptName'
                )
                        ) AS j
             INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
    WHERE pds2.deleted = 0
      AND pds2.dept_id IS NULL
      AND b2.dept_allocations IS NOT NULL
      AND JSON_LENGTH(b2.dept_allocations) > 0
      AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) = 'OBJECT'
      AND sd.dept_type = pds2.dept_type
)
  AND pds.id NOT IN (
    SELECT pds2.id
    FROM project_dept_service pds2
             INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
             INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
             INNER JOIN JSON_TABLE(
            b2.dept_allocations,
            '$[*]' COLUMNS(
                alloc_dept_id BIGINT PATH '$'
                )
                        ) AS j
             INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
    WHERE pds2.deleted = 0
      AND pds2.dept_id IS NULL
      AND b2.dept_allocations IS NOT NULL
      AND JSON_LENGTH(b2.dept_allocations) > 0
      AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) IN ('INTEGER', 'DOUBLE')
      AND sd.dept_type = pds2.dept_type
)
ORDER BY pds.id;


-- =============================================================================
-- 第二部分：确认第一部分结果后再执行（建议事务 + 备份）
-- 不要与第一部分在同一客户端「整文件执行」，请复制下面单独执行。
-- =============================================================================

/*
-- 新格式 [{"deptId":...}, ...]
UPDATE project_dept_service pds
    INNER JOIN project p ON p.id = pds.project_id AND p.deleted = 0
    INNER JOIN (
        SELECT pds2.id AS pds_pk,
               MIN(j.alloc_dept_id) AS chosen_dept_id,
               ANY_VALUE(COALESCE(NULLIF(j.alloc_dept_name, ''), sd.name)) AS chosen_dept_name
        FROM project_dept_service pds2
                 INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
                 INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
                 INNER JOIN JSON_TABLE(
                b2.dept_allocations,
                '$[*]' COLUMNS(
                    alloc_dept_id BIGINT PATH '$.deptId',
                    alloc_dept_name VARCHAR(256) PATH '$.deptName'
                    )
                            ) AS j
                 INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
        WHERE pds2.deleted = 0
          AND pds2.dept_id IS NULL
          AND b2.dept_allocations IS NOT NULL
          AND JSON_LENGTH(b2.dept_allocations) > 0
          AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) = 'OBJECT'
          AND sd.dept_type = pds2.dept_type
        GROUP BY pds2.id
    ) x ON x.pds_pk = pds.id
SET pds.dept_id     = x.chosen_dept_id,
    pds.dept_name   = x.chosen_dept_name,
    pds.update_time = NOW()
WHERE pds.dept_id IS NULL;

-- 旧格式 [119,117]（纯数字）
UPDATE project_dept_service pds
    INNER JOIN project p ON p.id = pds.project_id AND p.deleted = 0
    INNER JOIN (
        SELECT pds2.id AS pds_pk,
               MIN(j.alloc_dept_id) AS chosen_dept_id,
               ANY_VALUE(sd.name) AS chosen_dept_name
        FROM project_dept_service pds2
                 INNER JOIN project p2 ON p2.id = pds2.project_id AND p2.deleted = 0
                 INNER JOIN crm_business b2 ON b2.id = COALESCE(pds2.business_id, p2.business_id) AND b2.deleted = 0
                 INNER JOIN JSON_TABLE(
                b2.dept_allocations,
                '$[*]' COLUMNS(
                    alloc_dept_id BIGINT PATH '$'
                    )
                            ) AS j
                 INNER JOIN system_dept sd ON sd.id = j.alloc_dept_id AND sd.deleted = 0
        WHERE pds2.deleted = 0
          AND pds2.dept_id IS NULL
          AND b2.dept_allocations IS NOT NULL
          AND JSON_LENGTH(b2.dept_allocations) > 0
          AND JSON_TYPE(JSON_EXTRACT(b2.dept_allocations, '$[0]')) IN ('INTEGER', 'DOUBLE')
          AND sd.dept_type = pds2.dept_type
        GROUP BY pds2.id
    ) x ON x.pds_pk = pds.id
SET pds.dept_id     = x.chosen_dept_id,
    pds.dept_name   = x.chosen_dept_name,
    pds.update_time = NOW()
WHERE pds.dept_id IS NULL;
*/
