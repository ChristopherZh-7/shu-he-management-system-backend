-- =============================================
-- 合同管理改造 - 数据库升级脚本
-- 执行时间: 2026-01-16
-- 功能：支持两阶段合同管理（录入→领取→完善→审批→创建项目）
-- =============================================

-- 1. 添加新字段到合同表
ALTER TABLE crm_contract 
ADD COLUMN attachment VARCHAR(512) COMMENT '合同附件URL' AFTER remark,
ADD COLUMN assign_dept_ids VARCHAR(512) COMMENT '分派部门IDs（JSON数组）' AFTER attachment,
ADD COLUMN claim_status TINYINT DEFAULT 0 COMMENT '领取状态：0=待领取，1=已领取' AFTER assign_dept_ids,
ADD COLUMN claim_user_id BIGINT COMMENT '领取人用户ID' AFTER claim_status,
ADD COLUMN claim_time DATETIME COMMENT '领取时间' AFTER claim_user_id;

-- 2. 添加合同领取状态字典
INSERT INTO system_dict_type (name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES
('合同领取状态', 'crm_contract_claim_status', 0, '', '1', NOW(), '1', NOW(), 0, '1970-01-01 00:00:00');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(1, '待领取', '0', 'crm_contract_claim_status', 0, 'warning', '', '', '1', NOW(), '1', NOW(), 0),
(2, '已领取', '1', 'crm_contract_claim_status', 0, 'success', '', '', '1', NOW(), '1', NOW(), 0);

-- 3. 更新现有合同数据（如果有的话），默认为已领取状态
UPDATE crm_contract SET claim_status = 1 WHERE claim_status IS NULL OR claim_status = 0;

-- 4. 验证
DESCRIBE crm_contract;
SELECT * FROM system_dict_data WHERE dict_type = 'crm_contract_claim_status' AND deleted = 0;

SELECT '✅ 合同表升级完成！' AS result;
