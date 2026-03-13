-- ============================================================
-- BPM 流程模型发布失败诊断脚本
-- 用于排查 "流程不存在" (validateBpmnXml MODEL_NOT_EXISTS) 问题
-- ============================================================

-- 1. 检查合同审批模型在 act_re_model 中的记录
SELECT 
    ID_ AS model_id,
    NAME_ AS model_name,
    KEY_ AS model_key,
    EDITOR_SOURCE_VALUE_ID_ AS bpmn_xml_id,
    EDITOR_SOURCE_EXTRA_VALUE_ID_ AS simple_model_id,
    CASE 
        WHEN EDITOR_SOURCE_VALUE_ID_ IS NULL THEN '❌ 缺失'
        ELSE '✓ 有'
    END AS bpmn_status,
    CASE 
        WHEN EDITOR_SOURCE_EXTRA_VALUE_ID_ IS NULL THEN '❌ 缺失'
        ELSE '✓ 有'
    END AS simple_model_status
FROM act_re_model 
WHERE ID_ = 'a669251a-f778-11f0-a436-005056c00001'
   OR KEY_ = 'crm-contract-audit';

-- 2. 检查 BPMN XML 是否存在于 act_ge_bytearray
SELECT 
    b.ID_,
    b.NAME_,
    b.DEPLOYMENT_ID_,
    LENGTH(b.BYTES_) AS byte_length,
    CASE 
        WHEN b.ID_ IS NULL THEN '❌ 记录不存在'
        WHEN b.BYTES_ IS NULL OR LENGTH(b.BYTES_) = 0 THEN '❌ 内容为空'
        ELSE '✓ 正常'
    END AS status
FROM act_re_model m
LEFT JOIN act_ge_bytearray b ON m.EDITOR_SOURCE_VALUE_ID_ = b.ID_
WHERE m.ID_ = 'a669251a-f778-11f0-a436-005056c00001';

-- 3. 检查 Simple Model JSON 是否存在于 act_ge_bytearray
SELECT 
    b.ID_,
    b.NAME_,
    b.DEPLOYMENT_ID_,
    LENGTH(b.BYTES_) AS byte_length,
    CASE 
        WHEN b.ID_ IS NULL THEN '❌ 记录不存在'
        WHEN b.BYTES_ IS NULL OR LENGTH(b.BYTES_) = 0 THEN '❌ 内容为空'
        ELSE '✓ 正常'
    END AS status
FROM act_re_model m
LEFT JOIN act_ge_bytearray b ON m.EDITOR_SOURCE_EXTRA_VALUE_ID_ = b.ID_
WHERE m.ID_ = 'a669251a-f778-11f0-a436-005056c00001';

-- 4. 检查是否有 DEPLOYMENT_ID_ IS NULL 的 bytearray 被误删
--    (clean_test_data.sql 中的 DELETE 会删掉模型编辑器源数据！)
SELECT 
    COUNT(*) AS model_source_count,
    '若为0则可能被 clean_test_data.sql 误删' AS note
FROM act_ge_bytearray 
WHERE DEPLOYMENT_ID_ IS NULL;
