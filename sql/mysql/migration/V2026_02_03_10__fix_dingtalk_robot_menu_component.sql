-- =============================================
-- 修复钉钉群机器人菜单：清理重复记录 + 修正组件路径
-- 问题1：component 路径大小写与前端文件夹不匹配
-- 问题2：可能存在重复的菜单记录
-- =============================================

-- 步骤1：找出要保留的菜单ID（最小的ID）
SET @keep_id = (
  SELECT MIN(id) FROM `system_menu` 
  WHERE `name` = '群机器人管理' AND `type` = 2 AND `deleted` = 0
);

-- 步骤2：软删除重复的"群机器人管理"菜单（保留ID最小的那条）
UPDATE `system_menu` 
SET `deleted` = 1, `update_time` = NOW()
WHERE `name` = '群机器人管理' 
  AND `type` = 2
  AND `deleted` = 0
  AND `id` <> IFNULL(@keep_id, 0);

-- 步骤3：软删除孤儿子菜单（parent_id 指向已删除的菜单）
UPDATE `system_menu` m1
SET m1.`deleted` = 1, m1.`update_time` = NOW()
WHERE m1.`deleted` = 0
  AND EXISTS (
    SELECT 1 FROM (
      SELECT id FROM `system_menu` 
      WHERE `name` = '群机器人管理' AND `deleted` = 1
    ) m2 WHERE m2.id = m1.parent_id
  );

-- 步骤4：修正保留菜单的组件路径
UPDATE `system_menu` 
SET `component` = 'system/dingtalkrobot/index',
    `update_time` = NOW()
WHERE `name` = '群机器人管理' 
  AND `type` = 2
  AND `deleted` = 0;
