-- 为 crm_business 表新增提前投入申请详情字段
ALTER TABLE crm_business
    ADD COLUMN early_investment_personnel      TEXT           DEFAULT NULL COMMENT '提前投入人员（JSON: [{userId,userName,workDays}]）',
    ADD COLUMN early_investment_estimated_cost DECIMAL(20, 2) DEFAULT NULL COMMENT '预计自垫资金（元）',
    ADD COLUMN early_investment_work_scope     TEXT           DEFAULT NULL COMMENT '提前投入工作内容',
    ADD COLUMN early_investment_plan_start     DATE           DEFAULT NULL COMMENT '计划开始日期',
    ADD COLUMN early_investment_plan_end       DATE           DEFAULT NULL COMMENT '计划结束日期',
    ADD COLUMN early_investment_risk_handling  VARCHAR(500)   DEFAULT NULL COMMENT '若合同未签的处理方式',
    ADD COLUMN early_investment_reason         VARCHAR(1000)  DEFAULT NULL COMMENT '申请理由';
