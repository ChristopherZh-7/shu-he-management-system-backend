-- =====================================================
-- 修复数据迁移脚本（解决 only_full_group_by 问题）
-- 日期: 2026-01-16
-- =====================================================

-- 由于前面的步骤已执行成功（创建project表、添加project_id字段和索引），
-- 这里只需要执行数据迁移部分

-- 方案：为每个现有的 project_info 记录创建一个对应的项目
-- 而不是尝试按 (customer_id, contract_id, dept_type) 分组

-- 4. 数据迁移：为现有的服务项创建对应的项目记录（每个服务项对应一个项目）
INSERT INTO `project` (`name`, `code`, `dept_type`, `customer_id`, `customer_name`, `contract_id`, `contract_no`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 
    CONCAT(COALESCE(pi.customer_name, '未知客户'), '-项目') AS `name`,
    CONCAT('PRJ-', pi.dept_type, '-', DATE_FORMAT(pi.create_time, '%Y%m%d'), '-', LPAD(pi.id, 4, '0')) AS `code`,
    pi.dept_type,
    pi.customer_id,
    pi.customer_name,
    pi.contract_id,
    pi.contract_no,
    CASE WHEN pi.status = 3 THEN 2 WHEN pi.status IN (1, 2) THEN 1 ELSE 0 END AS `status`,
    pi.creator,
    pi.create_time,
    pi.updater,
    pi.update_time,
    0,
    pi.tenant_id
FROM `project_info` pi
WHERE pi.deleted = 0;

-- 5. 更新 project_info 表的 project_id 关联（根据 code 匹配）
UPDATE `project_info` pi
INNER JOIN `project` p ON p.code = CONCAT('PRJ-', pi.dept_type, '-', DATE_FORMAT(pi.create_time, '%Y%m%d'), '-', LPAD(pi.id, 4, '0'))
SET pi.project_id = p.id
WHERE pi.deleted = 0;
