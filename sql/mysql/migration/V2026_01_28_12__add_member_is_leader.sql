-- 安全运营人员表增加项目负责人标识
-- 项目负责人也是管理人员，通过 is_leader 字段区分

-- 1. 给 security_operation_member 表添加 is_leader 字段
ALTER TABLE `security_operation_member` 
ADD COLUMN `is_leader` TINYINT(1) DEFAULT 0 COMMENT '是否项目负责人：0否 1是' AFTER `member_type`;

-- 2. 把现有的项目负责人数据迁移到 member 表
-- 遍历所有安全运营合同，把 manager_id 对应的人员添加到 member 表
INSERT INTO `security_operation_member` (
    `so_contract_id`,
    `user_id`,
    `user_name`,
    `member_type`,
    `is_leader`,
    `start_date`,
    `status`,
    `remark`,
    `creator`,
    `create_time`,
    `updater`,
    `update_time`,
    `deleted`,
    `tenant_id`
)
SELECT 
    soc.id AS so_contract_id,
    soc.manager_id AS user_id,
    soc.manager_name AS user_name,
    1 AS member_type,  -- 管理人员
    1 AS is_leader,    -- 是项目负责人
    soc.onsite_start_date AS start_date,  -- 使用合同开始日期作为入职日期
    1 AS status,       -- 在岗
    '项目负责人（数据迁移）' AS remark,
    soc.creator,
    soc.create_time,
    soc.updater,
    soc.update_time,
    soc.deleted,
    soc.tenant_id
FROM `security_operation_contract` soc
WHERE soc.manager_id IS NOT NULL
  AND soc.deleted = 0
  -- 排除已存在的记录（避免重复插入）
  AND NOT EXISTS (
      SELECT 1 FROM `security_operation_member` som 
      WHERE som.so_contract_id = soc.id 
        AND som.user_id = soc.manager_id 
        AND som.is_leader = 1
        AND som.deleted = 0
  );

-- 3. 添加索引
ALTER TABLE `security_operation_member` ADD INDEX `idx_is_leader` (`is_leader`);

-- 验证迁移结果
-- SELECT soc.id, soc.manager_name, som.user_name, som.is_leader 
-- FROM security_operation_contract soc
-- LEFT JOIN security_operation_member som ON soc.id = som.so_contract_id AND som.is_leader = 1 AND som.deleted = 0
-- WHERE soc.deleted = 0;
