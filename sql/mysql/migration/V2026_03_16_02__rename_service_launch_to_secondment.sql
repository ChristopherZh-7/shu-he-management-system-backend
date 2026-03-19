-- =====================================================
-- 服务发起 → 借调申请 改名
-- 1. 菜单名称
-- 2. 权限按钮名称
-- 3. BPM 分类名称
-- 日期: 2026-03-16
-- =====================================================

-- 1. 更新主菜单名称（服务发起 / 跨部门服务申请 → 借调申请）
UPDATE system_menu
SET name = '借调申请'
WHERE (name = '服务发起' OR name = '跨部门服务申请')
  AND (path = 'service-launch' OR component_name = 'ServiceLaunchList')
  AND deleted = b'0';

-- 2. 更新权限按钮名称
UPDATE system_menu
SET name = '发起借调'
WHERE permission = 'project:service-launch:create' AND deleted = b'0';

UPDATE system_menu
SET name = '查看借调申请'
WHERE permission = 'project:service-launch:query' AND deleted = b'0';

UPDATE system_menu
SET name = '删除借调申请'
WHERE permission = 'project:service-launch:delete' AND deleted = b'0';

-- 3. 更新 BPM 流程分类名称（服务发起 → 借调申请）
UPDATE bpm_category
SET name = '借调申请'
WHERE (name = '服务发起' OR name = '跨部门服务申请')
  AND (code LIKE 'service_launch%')
  AND deleted = b'0';
