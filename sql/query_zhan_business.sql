-- 詹裕文(226) 商机转合同权限诊断
-- 1. 含医院的商机
SELECT b.id, b.name, b.owner_user_id, u.nickname, b.customer_id, c.name as cust_name
FROM crm_business b
LEFT JOIN system_users u ON b.owner_user_id = u.id
LEFT JOIN crm_customer c ON b.customer_id = c.id
WHERE b.deleted=0 AND (b.name LIKE '%医院%' OR c.name LIKE '%医院%');

-- 2. 上述商机的 crm_permission (biz_type=4 为商机)
SELECT cp.biz_id as business_id, cp.user_id, u.nickname, cp.level, 
  CASE cp.level WHEN 1 THEN '负责人' WHEN 2 THEN '只读' WHEN 3 THEN '读写' END as level_name
FROM crm_permission cp
LEFT JOIN system_users u ON cp.user_id = u.id
WHERE cp.biz_type = 4 AND cp.deleted = 0
  AND cp.biz_id IN (SELECT b.id FROM crm_business b LEFT JOIN crm_customer c ON b.customer_id=c.id 
    WHERE b.deleted=0 AND (b.name LIKE '%医院%' OR c.name LIKE '%医院%'));

-- 3. 詹裕文(226) 的角色
SELECT r.id, r.code, r.name FROM system_user_role ur 
JOIN system_role r ON ur.role_id = r.id WHERE ur.user_id = 226;
