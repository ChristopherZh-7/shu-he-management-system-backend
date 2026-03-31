-- =============================================================================
-- DANGEROUS: 清空 Flowable 全部历史（ACT_HI_*），流程实例管理 / 任务历史页将为空
-- 不影响：流程定义 ACT_RE_*、模型、表单；运行中 ACT_RU_* 若存在会一并无历史可查
-- 执行前请备份数据库。
-- =============================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM ACT_HI_VARINST;
DELETE FROM ACT_HI_DETAIL;
DELETE FROM ACT_HI_COMMENT;
DELETE FROM ACT_HI_ATTACHMENT;
DELETE FROM ACT_HI_TSK_LOG;
DELETE FROM ACT_HI_TASKINST;
DELETE FROM ACT_HI_ACTINST;
DELETE FROM ACT_HI_IDENTITYLINK;
DELETE FROM ACT_HI_ENTITYLINK;
DELETE FROM ACT_HI_PROCINST;

SET FOREIGN_KEY_CHECKS = 1;
