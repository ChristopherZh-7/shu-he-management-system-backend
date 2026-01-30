-- 更新外出费用记录表中的合同名称字段（填充已存在但为空的记录）
UPDATE outside_cost_record ocr
LEFT JOIN crm_contract c ON ocr.contract_id = c.id AND c.deleted = 0
SET ocr.contract_name = c.name
WHERE ocr.contract_name IS NULL OR ocr.contract_name = '';
