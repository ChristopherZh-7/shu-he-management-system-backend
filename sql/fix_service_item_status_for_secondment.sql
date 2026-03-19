-- 借调申请：将合同 10 下的服务项设为「进行中」以便可选
-- 根因：getServiceItemListByContract 只返回 status=1（进行中）的服务项
-- 当前：id=1 应急演练、id=2 安全监测 均为 status=0（草稿），接口返回空列表，导致「选择需要借人的单位」不可选
--
-- 说明：
-- - 应急演练(id=1)：serviceMemberType=2(管理服务项)，借调可用
-- - 安全监测(id=2)：serviceMemberType=1(驻场)，借调会排除驻场，即使 status=1 也不会出现在列表
-- 因此只更新 应急演练 即可

-- 将 应急演练 设为进行中
UPDATE project_service_item
SET status = 1,
    progress = 0,
    actual_start_time = COALESCE(actual_start_time, NOW()),
    updater = '1',
    update_time = NOW()
WHERE id = 1
  AND project_id = 2
  AND status = 0;

-- 验证
SELECT id, name, status, service_member_type, dept_type
FROM project_service_item
WHERE project_id = 2;
