-- =============================================
-- 将「总经办」设为所有一级部门的上级
--
-- 原因：商机审批链从发起人部门逐级向上直到总经办。
--       若总经办与安全运营、人事行政等为平级（都 parent_id=0），
--       审批链到根就结束，可能无法正确到达总经办。
--
-- 本脚本：将 安全运营服务部、人事行政部、安全技术服务部、数据安全服务部
--         的 parent_id 改为 总经办 的 id，使总经办成为它们的上级。
-- =============================================

SET NAMES utf8mb4;

SET @zongjingban_id = (SELECT id FROM system_dept WHERE name = '总经办' AND deleted = 0 LIMIT 1);

-- 将 安全运营服务部、人事行政部、安全技术服务部、数据安全服务部 的上级改为总经办
UPDATE system_dept
SET parent_id = @zongjingban_id, updater = '1', update_time = NOW()
WHERE parent_id = 0
  AND id != @zongjingban_id
  AND name IN ('安全运营服务部', '人事行政部', '安全技术服务部', '数据安全服务部')
  AND deleted = 0
  AND @zongjingban_id IS NOT NULL;

SELECT CONCAT('Done. 总经办(id=', @zongjingban_id, ') is now parent of 安全运营/人事行政/安全技术/数据安全.') AS result;
