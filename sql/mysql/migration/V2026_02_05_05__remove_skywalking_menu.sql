-- ========================================
-- 1. 删除 SkyWalking 等监控菜单
-- 2. 恢复定时任务菜单
-- ========================================

-- 1. 删除所有监控相关菜单（软删除）
UPDATE system_menu 
SET deleted = 1, update_time = NOW()
WHERE path LIKE '%monitors%' 
   OR path LIKE '%skywalking%' 
   OR path LIKE '%druid%' 
   OR path LIKE '%spring-boot-admin%'
   OR name LIKE '%SkyWalking%'
   OR name LIKE '%Druid%';

-- 2. 恢复定时任务菜单（如果已被删除，则恢复）
UPDATE system_menu 
SET visible = b'1', deleted = b'0', update_time = NOW()
WHERE id = 110 OR path = 'job' OR name = '定时任务';

-- 3. 如果定时任务菜单完全不存在，则插入（使用原始ID=110）
INSERT IGNORE INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    110, '定时任务', '', 2, 7, 2, 'job', 'fa-solid:tasks', 'infra/job/index', 'InfraJob',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
);

-- 4. 恢复调度日志菜单
UPDATE system_menu 
SET visible = b'1', deleted = b'0', update_time = NOW()
WHERE id = 111 OR path = 'job-log' OR name = '调度日志';

-- 5. 如果调度日志菜单不存在，则插入（使用原始ID=111）
INSERT IGNORE INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    111, '调度日志', '', 2, 8, 110, 'log', 'fa:tasks', 'infra/job/logger/index', 'InfraJobLog',
    0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
);

-- 6. 恢复定时任务相关的操作权限菜单（如果被删除）
UPDATE system_menu 
SET deleted = b'0', update_time = NOW()
WHERE parent_id = 110 OR parent_id = 111;
