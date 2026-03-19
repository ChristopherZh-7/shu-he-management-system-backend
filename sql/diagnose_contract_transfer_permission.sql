-- ============================================================
-- 南山合同转移权限问题诊断脚本
-- 在 DBeaver/Navicat/MySQL Workbench 中连接 10.40.88.37 执行
-- ============================================================

-- 1. 查找南山相关合同
SELECT 
    c.id AS contract_id,
    c.name AS contract_name,
    c.owner_user_id,
    u.nickname AS owner_nickname,
    c.customer_id,
    cust.name AS customer_name
FROM crm_contract c
LEFT JOIN system_users u ON c.owner_user_id = u.id
LEFT JOIN crm_customer cust ON c.customer_id = cust.id
WHERE c.deleted = 0 
  AND (c.name LIKE '%南山%' OR cust.name LIKE '%南山%');

-- 2. 查找詹姓用户
SELECT id, username, nickname, dept_id 
FROM system_users 
WHERE deleted = 0 AND (nickname LIKE '%詹%' OR username LIKE '%詹%');

-- 3. 对上面查到的合同ID，替换 <contract_id> 后执行（例如合同ID=23）：
-- 查该合同的 crm_permission 权限记录
/*
SELECT 
    cp.id, cp.biz_type, cp.biz_id, cp.user_id, cp.level,
    u.nickname,
    CASE cp.level WHEN 1 THEN '负责人' WHEN 2 THEN '只读' WHEN 3 THEN '读写' ELSE '未知' END AS level_name
FROM crm_permission cp
LEFT JOIN system_users u ON cp.user_id = u.id
WHERE cp.biz_type = 5 AND cp.biz_id = <contract_id>;  -- biz_type=5 为合同
*/

-- 4. 查詹的用户角色（替换 <user_id> 为詹的ID）
/*
SELECT u.id, u.nickname, r.id AS role_id, r.code AS role_code, r.name AS role_name
FROM system_users u
JOIN system_user_role ur ON u.id = ur.user_id
JOIN system_role r ON ur.role_id = r.id
WHERE u.id = <user_id>;
*/

-- 5. 一键诊断：南山合同 + 权限 + 负责人
SELECT 
    c.id AS contract_id,
    c.name AS contract_name,
    c.owner_user_id AS contract_owner_id,
    owner_u.nickname AS contract_owner_name,
    cp.user_id AS perm_user_id,
    perm_u.nickname AS perm_user_name,
    cp.level,
    CASE cp.level WHEN 1 THEN '负责人' WHEN 2 THEN '只读' WHEN 3 THEN '读写' ELSE '-' END AS perm_level_name
FROM crm_contract c
LEFT JOIN system_users owner_u ON c.owner_user_id = owner_u.id
LEFT JOIN crm_permission cp ON cp.biz_type = 5 AND cp.biz_id = c.id
LEFT JOIN system_users perm_u ON cp.user_id = perm_u.id
WHERE c.deleted = 0 
  AND (c.name LIKE '%南山%' OR EXISTS (
      SELECT 1 FROM crm_customer cust 
      WHERE cust.id = c.customer_id AND cust.deleted = 0 AND cust.name LIKE '%南山%'
  ));
