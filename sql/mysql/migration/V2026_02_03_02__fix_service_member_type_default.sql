-- 修复安全运营服务项的默认值
-- 将现有服务项设为管理服务项(2)，以便可以被服务发起
UPDATE `project_info` SET `service_member_type` = 2 WHERE `dept_type` = 2 AND `deleted` = 0;
