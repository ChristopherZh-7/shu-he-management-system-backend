-- =====================================================
-- 工单中心 - 验收闭环 + 重开 + 拒单退回（二期）
--   - 状态机变化：finish 1→2 待验收；review_pass 2→3；review_reject 2→1
--   - 新状态：6 已退回（return 0→6 / resubmit 6→0 / cancel 6→5）
--   - 主表新增验收人 / 验收时间 / 验收意见 / 重开次数 / 退回原因字段
-- =====================================================

ALTER TABLE `shuhe_ticket`
    ADD COLUMN `reviewer_id`    BIGINT       DEFAULT NULL COMMENT '验收人ID' AFTER `close_time`,
    ADD COLUMN `reviewer_name`  VARCHAR(64)  DEFAULT NULL COMMENT '验收人姓名快照' AFTER `reviewer_id`,
    ADD COLUMN `review_time`    DATETIME     DEFAULT NULL COMMENT '验收时间' AFTER `reviewer_name`,
    ADD COLUMN `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '验收意见（通过评价/驳回原因）' AFTER `review_time`,
    ADD COLUMN `reopen_count`   INT          NOT NULL DEFAULT 0 COMMENT '重开次数' AFTER `review_comment`,
    ADD COLUMN `return_reason`  VARCHAR(500) DEFAULT NULL COMMENT '拒单退回原因' AFTER `reopen_count`;

ALTER TABLE `shuhe_ticket`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2待验收 3已完成 4已关闭 5已取消 6已退回';
