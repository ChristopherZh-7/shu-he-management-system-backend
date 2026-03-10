-- =============================================
-- BPM 流程定义：支持按角色、岗位配置发起人和管理员
-- 1. 可发起角色编号数组 start_role_ids
-- 2. 可发起岗位编号数组 start_post_ids
-- 3. 可管理角色编号数组 manager_role_ids
-- 4. 可管理岗位编号数组 manager_post_ids
-- =============================================

ALTER TABLE bpm_process_definition_info
    ADD COLUMN start_role_ids varchar(256) DEFAULT NULL COMMENT '可发起角色编号数组' AFTER start_dept_ids,
    ADD COLUMN start_post_ids varchar(256) DEFAULT NULL COMMENT '可发起岗位编号数组' AFTER start_role_ids,
    ADD COLUMN manager_role_ids varchar(256) DEFAULT NULL COMMENT '可管理角色编号数组' AFTER manager_user_ids,
    ADD COLUMN manager_post_ids varchar(256) DEFAULT NULL COMMENT '可管理岗位编号数组' AFTER manager_role_ids;
