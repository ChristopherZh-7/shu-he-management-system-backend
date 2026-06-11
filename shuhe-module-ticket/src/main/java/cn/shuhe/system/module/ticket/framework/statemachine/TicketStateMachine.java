package cn.shuhe.system.module.ticket.framework.statemachine;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_STATUS_INVALID;

/**
 * 工单状态机（线程安全 · 纯静态工具）。
 *
 * <p>对应设计文档 {@code docs/design/ticket-design.md} §1。所有状态变更动作（assign / start /
 * finish / review / close / cancel / transfer / reopen / return / resubmit）的入口都必须先调
 * {@link #checkTransition(Integer, TicketActionEnum)} 校验，否则直接抛
 * {@link ServiceException} {@code TICKET_STATUS_INVALID}。
 *
 * <p>二期验收闭环已激活：finish 进入 2 待验收，由提单人 review_pass / review_reject 验收；
 * 0 待处理可被部门负责人 return 退回，提单人 resubmit 重新提交。
 */
public final class TicketStateMachine {

    private TicketStateMachine() {}

    /** action → (fromStatus → toStatus) 转换矩阵。 */
    private static final Map<TicketActionEnum, Map<Integer, Integer>> TRANSITIONS = new HashMap<>();

    static {
        // assign：仅在 0 待处理 时允许，目标仍为 0（只改 assignee_id 不改状态）
        put(TicketActionEnum.ASSIGN, TicketStatusEnum.PENDING, TicketStatusEnum.PENDING);

        // accept：0 → 1（主管接单，同时指定执行人；与 start 区分：start 是 assignee 本人开干，accept 是主管派工）
        put(TicketActionEnum.ACCEPT, TicketStatusEnum.PENDING, TicketStatusEnum.IN_PROGRESS);

        // start：0 → 1（处理人接单）
        put(TicketActionEnum.START, TicketStatusEnum.PENDING, TicketStatusEnum.IN_PROGRESS);

        // submit_review：1 → 2（与 finish 等价，保留兼容）
        put(TicketActionEnum.SUBMIT_REVIEW, TicketStatusEnum.IN_PROGRESS, TicketStatusEnum.PENDING_REVIEW);

        // review_pass：2 → 3（提单人验收通过）
        put(TicketActionEnum.REVIEW_PASS, TicketStatusEnum.PENDING_REVIEW, TicketStatusEnum.COMPLETED);

        // review_reject：2 → 1（提单人驳回，退回原执行人重做）
        put(TicketActionEnum.REVIEW_REJECT, TicketStatusEnum.PENDING_REVIEW, TicketStatusEnum.IN_PROGRESS);

        // finish：1 → 2（执行人提交结果后进入待验收，由提单人 review_pass/review_reject 闭环）
        put(TicketActionEnum.FINISH, TicketStatusEnum.IN_PROGRESS, TicketStatusEnum.PENDING_REVIEW);

        // close：3 → 4
        put(TicketActionEnum.CLOSE, TicketStatusEnum.COMPLETED, TicketStatusEnum.CLOSED);

        // reopen：3 / 4 → 1（提单人窗口期内重新打开；窗口/次数校验在 Service 层）
        put(TicketActionEnum.REOPEN, TicketStatusEnum.COMPLETED, TicketStatusEnum.IN_PROGRESS);
        put(TicketActionEnum.REOPEN, TicketStatusEnum.CLOSED, TicketStatusEnum.IN_PROGRESS);

        // return：0 → 6（部门负责人拒单退回提单人）
        put(TicketActionEnum.RETURN, TicketStatusEnum.PENDING, TicketStatusEnum.RETURNED);

        // resubmit：6 → 0（提单人修改后重新提交）
        put(TicketActionEnum.RESUBMIT, TicketStatusEnum.RETURNED, TicketStatusEnum.PENDING);

        // cancel：0 → 5（待处理直接取消）；6 → 5（已退回放弃）
        put(TicketActionEnum.CANCEL, TicketStatusEnum.PENDING, TicketStatusEnum.CANCELLED);
        put(TicketActionEnum.CANCEL, TicketStatusEnum.RETURNED, TicketStatusEnum.CANCELLED);

        // transfer：0/1/2 任意非终态状态都允许，状态不变（只改 assignee_id）
        put(TicketActionEnum.TRANSFER, TicketStatusEnum.PENDING, TicketStatusEnum.PENDING);
        put(TicketActionEnum.TRANSFER, TicketStatusEnum.IN_PROGRESS, TicketStatusEnum.IN_PROGRESS);
        put(TicketActionEnum.TRANSFER, TicketStatusEnum.PENDING_REVIEW, TicketStatusEnum.PENDING_REVIEW);

        // comment：任意状态都允许，状态不变（不在本工具内强校验，由 Service 决定）
    }

    private static void put(TicketActionEnum action, TicketStatusEnum from, TicketStatusEnum to) {
        TRANSITIONS.computeIfAbsent(action, k -> new HashMap<>())
                .put(from.getStatus(), to.getStatus());
    }

    /**
     * 计算目标状态。
     *
     * @return 目标状态值；不允许时返回 {@code null}
     */
    public static Integer nextStatus(Integer fromStatus, TicketActionEnum action) {
        if (fromStatus == null || action == null) {
            return null;
        }
        Map<Integer, Integer> map = TRANSITIONS.get(action);
        if (map == null) {
            return null;
        }
        return map.get(fromStatus);
    }

    /**
     * 校验转换合法性，失败时抛 {@link ServiceException}。
     *
     * @return 目标状态（调用方使用）
     */
    public static Integer checkTransition(Integer fromStatus, TicketActionEnum action) {
        Integer toStatus = nextStatus(fromStatus, action);
        if (toStatus == null) {
            String fromName = TicketStatusEnum.nameOf(fromStatus);
            String actionName = action == null ? "未知" : action.getName();
            throw ServiceExceptionUtil.exception(TICKET_STATUS_INVALID,
                    fromName == null ? String.valueOf(fromStatus) : fromName,
                    actionName);
        }
        return toStatus;
    }

    /**
     * 状态机判定结果，便于 Service 收集；{@code allowed=false} 时 {@code reason} 给出原因。
     */
    @Getter
    @RequiredArgsConstructor
    public static class Result {
        private final boolean allowed;
        private final Integer toStatus;
        private final String reason;
    }

    public static Result evaluate(Integer fromStatus, TicketActionEnum action) {
        Integer toStatus = nextStatus(fromStatus, action);
        if (toStatus == null) {
            return new Result(false, null,
                    "工单当前状态=" + TicketStatusEnum.nameOf(fromStatus)
                            + "，不允许" + (action == null ? "未知" : action.getName()) + "操作");
        }
        return new Result(true, toStatus, null);
    }
}
