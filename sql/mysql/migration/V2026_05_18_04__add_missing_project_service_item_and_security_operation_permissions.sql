-- =====================================================
-- Migration: V2026_05_18_04__add_missing_project_service_item_and_security_operation_permissions.sql
-- Date:      2026-05-18
-- Description:
--   续补 V2026_05_18_02 漏处理的权限继承。
--
--   背景：
--     V2026_05_18 merge 把 5081 / 5091 / 5166 三个旧分类菜单及其下挂的全部按钮软删，
--     在 5080 下重建了 3 组（每组各 4 个）新按钮：
--       - 5319/5320/5321/5322 → project:service-item:*
--       - 5323/5324/5325/5326 → project:security-operation:*
--       - 5330/5331/5332/5333 → project:project:*（V2026_05_18_01 已处理）
--       - 5337/5340 → project:site:*（V2026_05_18_02 已处理）
--       - 5341/5344 → project:dept-service:*（V2026_05_18_02 已处理）
--
--     V2026_05_18_02 的 Step 3 只把 site / dept-service 这 8 个新菜单授权给历史角色，
--     **遗漏了 service-item 和 security-operation 这 8 个新菜单**。导致：
--       - GET /admin-api/project/service-item/page  → 非超管角色 403（zhengyi/af_mg 实测堆栈）
--       - GET /admin-api/project/security-operation/*  → 非超管角色 403
--
--   gap 全景（执行本脚本前）：
--     permission                          auth_role_cnt   hist_role_cnt
--     project:service-item:query          1               8     ← 缺 7
--     project:service-item:create         1               8     ← 缺 7
--     project:service-item:update         1               8     ← 缺 7
--     project:service-item:delete         1               8     ← 缺 7
--     project:security-operation:query    1               4     ← 缺 3
--     project:security-operation:create   1               4     ← 缺 3
--     project:security-operation:update   1               4     ← 缺 3
--     project:security-operation:delete   1               4     ← 缺 3
--
--   本 migration 做一件事：
--     扫历史授权快照，把这 8 个新菜单授权给「历史上拥有 5080/5081/5091/5166 下任一
--     project:service-item:* 或 project:security-operation:* 按钮」的所有角色。
--
--   设计原则：与 V2026_05_18_02 完全一致。
--     - NOT EXISTS / INSERT IGNORE 防重，可重复执行
--     - @ready 守卫只在 5080 已合并完毕的库上跑
--     - 不修改任何已有 system_role_menu 记录
--     - 不动 system_menu 主表（5319-5326 V2026_05_18 merge 时已创建好）
--
--   重要：执行后必须 evict Redis 中的权限缓存才能立即生效！
--   evict 命令见脚本末尾。
-- =====================================================

-- ============================================================
-- 0. 防御：仅当 5080 已合并到 type=2（已被 V2026_05_18 处理过）才执行
-- ============================================================
SET @ready = (
    SELECT COUNT(*) FROM system_menu
    WHERE id = 5080 AND type = 2 AND deleted = b'0'
);

-- ============================================================
-- 1. 给超管 role_id=1 兜底授权（应该已经有，做幂等保险）
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT 1, m.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
   AND m.deleted = b'0'
   AND m.permission IN (
       'project:service-item:query', 'project:service-item:create',
       'project:service-item:update', 'project:service-item:delete',
       'project:security-operation:query', 'project:security-operation:create',
       'project:security-operation:update', 'project:security-operation:delete'
   )
   AND @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_role_menu rm
        WHERE rm.role_id = 1
          AND rm.menu_id = m.id
          AND rm.deleted = b'0'
   );

-- ============================================================
-- 2. 为「历史上拥有 5080/5081/5091/5166 下任一 service-item 或 security-operation 按钮」的所有角色
--    自动授权这 8 条新菜单
--    （扫历史授权快照，包含 deleted=1 的菜单，因为 5081/5091/5166 下的菜单已被软删）
-- ============================================================
INSERT IGNORE INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted)
SELECT DISTINCT rm_old.role_id, m_new.id, '1', NOW(), '1', NOW(), b'0'
  FROM system_role_menu rm_old
  JOIN system_menu m_old ON m_old.id = rm_old.menu_id
  JOIN system_menu m_new ON m_new.parent_id = 5080
                        AND m_new.type = 3
                        AND m_new.deleted = b'0'
                        AND m_new.permission IN (
                            'project:service-item:query', 'project:service-item:create',
                            'project:service-item:update', 'project:service-item:delete',
                            'project:security-operation:query', 'project:security-operation:create',
                            'project:security-operation:update', 'project:security-operation:delete'
                        )
 WHERE rm_old.deleted = b'0'
   AND m_old.parent_id IN (5080, 5081, 5091, 5166)
   AND m_old.type = 3
   AND m_old.permission = m_new.permission
   AND @ready = 1
   AND NOT EXISTS (
       SELECT 1 FROM system_role_menu rm_chk
        WHERE rm_chk.role_id = rm_old.role_id
          AND rm_chk.menu_id = m_new.id
          AND rm_chk.deleted = b'0'
   );

-- ============================================================
-- 3. 验证（执行后查看）
-- ============================================================
SELECT '===== service-item + security-operation 权限对齐完成 =====' AS msg;

-- 3.1 5080 下每个按钮当前授权角色数（应当 service-item:* >= 8，security-operation:* >= 4）
SELECT
    m.id, m.name, m.permission,
    (SELECT COUNT(DISTINCT rm.role_id) FROM system_role_menu rm WHERE rm.menu_id = m.id AND rm.deleted = b'0') AS auth_role_cnt
  FROM system_menu m
 WHERE m.parent_id = 5080
   AND m.type = 3
   AND m.deleted = b'0'
   AND m.permission LIKE 'project:%'
 ORDER BY m.sort, m.id;

-- 3.2 验 af_mg (role_id=164) 拿到的 12 个 project 按钮（应当 4+4+4=12，再加 site/dept-service 共 16）
SELECT
    CONCAT('af_mg (role_id=164) 拿到的 project:* 按钮数 = ',
           (SELECT COUNT(*)
              FROM system_role_menu rm
              JOIN system_menu m ON m.id = rm.menu_id
             WHERE rm.role_id = 164
               AND rm.deleted = b'0'
               AND m.deleted = b'0'
               AND m.permission LIKE 'project:%'
               AND m.parent_id = 5080),
           ' 个（期望 20：project*4 + service-item*4 + security-operation*4 + site*4 + dept-service*4）'
    ) AS af_mg_button_check;

-- ============================================================
-- 4. ⚠ 必跑：执行后清掉 Redis 权限缓存，立即生效
-- ============================================================
-- Spring Cache 不会感知到我们直接 INSERT system_role_menu 的变动，必须手动 evict
-- 在服务器 shell 执行：
--   redis-cli -n 0 --scan --pattern 'permission_menu_ids:project:service-item:*' | xargs -r redis-cli -n 0 UNLINK
--   redis-cli -n 0 --scan --pattern 'permission_menu_ids:project:security-operation:*' | xargs -r redis-cli -n 0 UNLINK
--   redis-cli -n 0 --scan --pattern 'menu_role_ids:5319' | xargs -r redis-cli -n 0 UNLINK
--   redis-cli -n 0 --scan --pattern 'menu_role_ids:5320' | xargs -r redis-cli -n 0 UNLINK
--   ... (5321/5322/5323/5324/5325/5326)
-- 一行兜底（清掉所有 menu_role_ids:* 和 permission_menu_ids:*，等下次访问自动重建）：
--   redis-cli -n 0 KEYS 'menu_role_ids:*' | xargs -r redis-cli -n 0 UNLINK
--   redis-cli -n 0 KEYS 'permission_menu_ids:*' | xargs -r redis-cli -n 0 UNLINK
-- 注意：UNLINK 是异步删除，KEYS 在大库会卡，生产环境用 --scan 替代

-- ============================================================
-- 一键回滚（如需）
-- ============================================================
-- 物理删除本次脚本插入的 8 个菜单授权行（仅限 creator='1' 且 menu_id 在 5319-5326）
-- DELETE FROM system_role_menu
--  WHERE menu_id IN (5319, 5320, 5321, 5322, 5323, 5324, 5325, 5326)
--    AND creator = '1'
--    AND create_time >= CURDATE()
--    AND create_time <  CURDATE() + INTERVAL 1 DAY;
