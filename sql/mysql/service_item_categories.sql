-- =============================================
-- 服务项分类数据
-- 执行时间: 2026-01-15
-- =============================================

-- 先清空现有分类（如果有的话）
DELETE FROM crm_product_category WHERE 1=1;

-- 添加一级分类
INSERT INTO crm_product_category (id, name, parent_id, sort, creator, create_time, updater, update_time, deleted) VALUES
(1, '安全服务', 0, 1, '1', NOW(), '1', NOW(), 0),
(2, '安全运维', 0, 2, '1', NOW(), '1', NOW(), 0),
(3, '安全咨询', 0, 3, '1', NOW(), '1', NOW(), 0),
(4, '安全培训', 0, 4, '1', NOW(), '1', NOW(), 0);

-- 添加二级分类 - 安全服务
INSERT INTO crm_product_category (id, name, parent_id, sort, creator, create_time, updater, update_time, deleted) VALUES
(11, '渗透测试', 1, 1, '1', NOW(), '1', NOW(), 0),
(12, '漏洞扫描', 1, 2, '1', NOW(), '1', NOW(), 0),
(13, '代码审计', 1, 3, '1', NOW(), '1', NOW(), 0),
(14, '安全加固', 1, 4, '1', NOW(), '1', NOW(), 0),
(15, '应急响应', 1, 5, '1', NOW(), '1', NOW(), 0),
(16, '红蓝对抗', 1, 6, '1', NOW(), '1', NOW(), 0);

-- 添加二级分类 - 安全运维
INSERT INTO crm_product_category (id, name, parent_id, sort, creator, create_time, updater, update_time, deleted) VALUES
(21, '安全值守', 2, 1, '1', NOW(), '1', NOW(), 0),
(22, '日志分析', 2, 2, '1', NOW(), '1', NOW(), 0),
(23, '威胁监测', 2, 3, '1', NOW(), '1', NOW(), 0),
(24, '设备运维', 2, 4, '1', NOW(), '1', NOW(), 0);

-- 添加二级分类 - 安全咨询
INSERT INTO crm_product_category (id, name, parent_id, sort, creator, create_time, updater, update_time, deleted) VALUES
(31, '等保测评', 3, 1, '1', NOW(), '1', NOW(), 0),
(32, '风险评估', 3, 2, '1', NOW(), '1', NOW(), 0),
(33, '安全规划', 3, 3, '1', NOW(), '1', NOW(), 0),
(34, '合规咨询', 3, 4, '1', NOW(), '1', NOW(), 0);

-- 添加二级分类 - 安全培训
INSERT INTO crm_product_category (id, name, parent_id, sort, creator, create_time, updater, update_time, deleted) VALUES
(41, '安全意识培训', 4, 1, '1', NOW(), '1', NOW(), 0),
(42, '技术培训', 4, 2, '1', NOW(), '1', NOW(), 0),
(43, '攻防演练培训', 4, 3, '1', NOW(), '1', NOW(), 0);

-- 查看结果
SELECT id, name, parent_id, sort FROM crm_product_category ORDER BY parent_id, sort;

SELECT '✅ 服务项分类已添加！' AS result;
