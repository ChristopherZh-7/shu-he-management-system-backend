-- 服务项状态：上架/下架 改为 开启/关闭

UPDATE `system_dict_data` 
SET `label` = '开启' 
WHERE `dict_type` = 'crm_product_status' AND `value` = '1';

UPDATE `system_dict_data` 
SET `label` = '关闭' 
WHERE `dict_type` = 'crm_product_status' AND `value` = '0';

-- 字典类型名称
UPDATE `system_dict_type` 
SET `name` = '服务项状态' 
WHERE `type` = 'crm_product_status';
