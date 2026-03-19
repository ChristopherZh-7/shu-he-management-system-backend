-- =====================================================
-- 借调申请 BPM 流程诊断 SQL
-- 在 MySQL 客户端执行，或通过 DBeaver/Navicat 等工具运行
-- 数据库: shuhe-ms (见 application-local.yaml)
-- =====================================================

-- 1. 检查 act_re_procdef 中 unified_service_launch 流程
SELECT 
    ID_, 
    KEY_, 
    VERSION_, 
    TENANT_ID_, 
    SUSPENSION_STATE_,
    CASE SUSPENSION_STATE_ 
        WHEN 1 THEN '激活' 
        WHEN 2 THEN '挂起' 
        ELSE '未知' 
    END AS 状态说明
FROM act_re_procdef 
WHERE KEY_ = 'unified_service_launch';

-- 2. 检查 bpm_process_definition_info 是否关联
SELECT 
    id,
    process_definition_id,
    form_id,
    category
FROM bpm_process_definition_info 
WHERE process_definition_id LIKE 'unified_service_launch%';

-- 3. 检查 act_re_deployment 部署记录
SELECT 
    ID_,
    NAME_,
    KEY_,
    DEPLOY_TIME_
FROM act_re_deployment 
WHERE KEY_ = 'unified_service_launch';
