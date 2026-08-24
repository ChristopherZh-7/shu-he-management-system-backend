package cn.shuhe.system.module.ticket.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单操作动作枚举。对应 `shuhe_ticket_log.action` 字段。
 *
 * <p>详细的 from→to 映射见 {@code docs/design/ticket-design.md} §1.3。
 */
@Getter
@AllArgsConstructor
public enum TicketActionEnum {

    CREATE("create", "创建工单"),
    ASSIGN("assign", "分派"),
    ACCEPT("accept", "接单"),
    START("start", "开始处理"),
    PM_REVIEW_PASS("pm_review_pass", "项目经理确认通过"),
    PM_REVIEW_REJECT("pm_review_reject", "项目经理退回"),
    SUBMIT_REVIEW("submit_review", "提交审核"),
    TECH_REVIEW_PASS("tech_review_pass", "技术审核通过"),
    TECH_REVIEW_REJECT("tech_review_reject", "技术审核驳回"),
    REVIEW_PASS("review_pass", "验收通过"),
    REVIEW_REJECT("review_reject", "验收驳回"),
    FINISH("finish", "完成"),
    CLOSE("close", "关闭"),
    REOPEN("reopen", "重新打开"),
    RETURN("return", "拒单退回"),
    RESUBMIT("resubmit", "重新提交"),
    CANCEL("cancel", "取消"),
    TRANSFER("transfer", "转交"),
    COMMENT("comment", "评论");

    private final String action;
    private final String name;

    public static TicketActionEnum of(String action) {
        if (action == null) {
            return null;
        }
        for (TicketActionEnum e : values()) {
            if (e.action.equals(action)) {
                return e;
            }
        }
        return null;
    }
}
