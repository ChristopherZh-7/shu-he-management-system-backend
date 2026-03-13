-- ============================================================
-- 恢复合同审批流程模型 (act_ge_bytearray 中缺失的 BPMN 和 Simple Model)
-- 当 diagnose_bpm_model.sql 的 2、3 步显示「记录不存在」时执行此脚本
-- ============================================================

-- 注意：若 act_re_model 中 EDITOR_SOURCE_VALUE_ID_ / EDITOR_SOURCE_EXTRA_VALUE_ID_ 为 NULL，
-- 需先执行下方「情况二」的 UPDATE，再执行插入

-- ========== 情况一：act_re_model 中已有引用 ID ==========
-- 从备份可知合同审批模型的 ID 为：407201bc-f80a-11f0-a992-005056c00001 (BPMN), 4074249d-f80a-11f0-a992-005056c00001 (Simple)

-- 插入 BPMN XML（发起人 -> 审批人(用户249) -> 结束）
INSERT IGNORE INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
VALUES (
    '407201bc-f80a-11f0-a992-005056c00001',
    1,
    'crm-contract-audit.bpmn',
    NULL,
    '<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <process id="crm-contract-audit" name="合同审批" isExecutable="true">
    <startEvent id="StartEvent" name="开始"></startEvent>
    <userTask id="StartUserNode" name="发起人">
      <extensionElements>
        <flowable:approveType>1</flowable:approveType>
      </extensionElements>
    </userTask>
    <userTask id="Activity__contract_approve" name="审批人">
      <extensionElements>
        <flowable:candidateStrategy>30</flowable:candidateStrategy>
        <flowable:candidateUsers>[249]</flowable:candidateUsers>
        <flowable:approveType>1</flowable:approveType>
      </extensionElements>
    </userTask>
    <endEvent id="EndEvent" name="结束"></endEvent>
    <sequenceFlow id="flow1" sourceRef="StartEvent" targetRef="StartUserNode"/>
    <sequenceFlow id="flow2" sourceRef="StartUserNode" targetRef="Activity__contract_approve"/>
    <sequenceFlow id="flow3" sourceRef="Activity__contract_approve" targetRef="EndEvent"/>
  </process>
</definitions>',
    NULL
);

-- 插入 Simple Model JSON（发起人 -> 审批人 -> 结束）
INSERT IGNORE INTO act_ge_bytearray (ID_, REV_, NAME_, DEPLOYMENT_ID_, BYTES_, GENERATED_)
VALUES (
    '4074249d-f80a-11f0-a992-005056c00001',
    1,
    'simple-model',
    NULL,
    '{"id":"StartUserNode","type":10,"name":"发起人","childNode":{"id":"Activity__contract_approve","type":11,"name":"审批人","candidateStrategy":30,"candidateParam":"[249]","approveType":1,"approveMethod":1,"childNode":{"id":"EndEvent","type":1,"name":"结束"}}}',
    NULL
);

-- ========== 情况二：act_re_model 中引用 ID 为 NULL ==========
-- 若诊断显示 bpmn_xml_id 和 simple_model_id 为 NULL，先执行上述 INSERT，再执行：
-- UPDATE act_re_model 
-- SET EDITOR_SOURCE_VALUE_ID_ = '407201bc-f80a-11f0-a992-005056c00001',
--     EDITOR_SOURCE_EXTRA_VALUE_ID_ = '4074249d-f80a-11f0-a992-005056c00001'
-- WHERE ID_ = 'a669251a-f778-11f0-a436-005056c00001';
