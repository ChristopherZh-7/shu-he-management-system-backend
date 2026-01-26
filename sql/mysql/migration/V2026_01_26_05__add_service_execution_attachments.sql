-- 服务执行申请添加渗透测试附件字段
-- V2026_01_26_05

-- 添加授权书附件URL字段
ALTER TABLE project_service_execution 
ADD COLUMN authorization_urls TEXT NULL COMMENT '授权书附件URL（JSON数组）' AFTER remark;

-- 添加测试范围附件URL字段
ALTER TABLE project_service_execution 
ADD COLUMN test_scope_urls TEXT NULL COMMENT '测试范围附件URL（JSON数组）' AFTER authorization_urls;

-- 添加账号密码附件URL字段
ALTER TABLE project_service_execution 
ADD COLUMN credentials_urls TEXT NULL COMMENT '账号密码附件URL（JSON数组）' AFTER test_scope_urls;
