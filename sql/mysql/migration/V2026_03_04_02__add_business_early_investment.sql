ALTER TABLE crm_business
    ADD COLUMN early_investment_status              TINYINT      DEFAULT NULL COMMENT '提前投入审批状态（0草稿/10审批中/20通过/30驳回）',
    ADD COLUMN early_investment_process_instance_id VARCHAR(64)  DEFAULT NULL COMMENT '提前投入BPM流程实例ID';
