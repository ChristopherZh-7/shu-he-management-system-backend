package cn.shuhe.system.module.ticket.framework.statemachine;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import org.junit.jupiter.api.Test;

import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TicketStateMachine} 单元测试 —— 纯静态工具，不依赖容器。
 *
 * <p>覆盖：
 * <ul>
 *     <li>合法 transition：assign / start / finish / review / close / cancel / transfer /
 *     reopen / return / resubmit</li>
 *     <li>非法 transition 抛 {@link ServiceException}（{@code TICKET_STATUS_INVALID}）</li>
 *     <li>{@link TicketStateMachine#evaluate} 双轨返回</li>
 *     <li>terminal 状态 + null 入参的边界</li>
 * </ul>
 */
class TicketStateMachineTest {

    @Test
    void nextStatus_assignAtPending_keepsPending() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.ASSIGN);
        assertEquals(TicketStatusEnum.PENDING.getStatus(), to);
    }

    @Test
    void nextStatus_startAtPending_movesToInProgress() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.START);
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), to);
    }

    @Test
    void nextStatus_acceptAtPending_movesToInProgress() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.ACCEPT);
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), to);
    }

    @Test
    void nextStatus_acceptAtInProgress_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(TicketStatusEnum.IN_PROGRESS.getStatus(), TicketActionEnum.ACCEPT));
    }

    @Test
    void nextStatus_finishAtInProgress_movesToPendingReview() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.IN_PROGRESS.getStatus(), TicketActionEnum.FINISH);
        assertEquals(TicketStatusEnum.PENDING_REVIEW.getStatus(), to);
    }

    @Test
    void nextStatus_reviewPassAtPendingReview_movesToCompleted() {
        Integer to = TicketStateMachine.nextStatus(
                TicketStatusEnum.PENDING_REVIEW.getStatus(), TicketActionEnum.REVIEW_PASS);
        assertEquals(TicketStatusEnum.COMPLETED.getStatus(), to);
    }

    @Test
    void nextStatus_reviewRejectAtPendingReview_movesBackToInProgress() {
        Integer to = TicketStateMachine.nextStatus(
                TicketStatusEnum.PENDING_REVIEW.getStatus(), TicketActionEnum.REVIEW_REJECT);
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), to);
    }

    @Test
    void nextStatus_reviewPassAtInProgress_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(
                TicketStatusEnum.IN_PROGRESS.getStatus(), TicketActionEnum.REVIEW_PASS));
    }

    @Test
    void nextStatus_closeAtCompleted_movesToClosed() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.COMPLETED.getStatus(), TicketActionEnum.CLOSE);
        assertEquals(TicketStatusEnum.CLOSED.getStatus(), to);
    }

    @Test
    void nextStatus_reopenAtCompletedOrClosed_movesToInProgress() {
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), TicketStateMachine.nextStatus(
                TicketStatusEnum.COMPLETED.getStatus(), TicketActionEnum.REOPEN));
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), TicketStateMachine.nextStatus(
                TicketStatusEnum.CLOSED.getStatus(), TicketActionEnum.REOPEN));
    }

    @Test
    void nextStatus_reopenAtReturned_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(
                TicketStatusEnum.RETURNED.getStatus(), TicketActionEnum.REOPEN));
    }

    @Test
    void nextStatus_returnAtPending_movesToReturned() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.RETURN);
        assertEquals(TicketStatusEnum.RETURNED.getStatus(), to);
    }

    @Test
    void nextStatus_returnAtInProgress_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(
                TicketStatusEnum.IN_PROGRESS.getStatus(), TicketActionEnum.RETURN));
    }

    @Test
    void nextStatus_resubmitAtReturned_movesToPending() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.RETURNED.getStatus(), TicketActionEnum.RESUBMIT);
        assertEquals(TicketStatusEnum.PENDING.getStatus(), to);
    }

    @Test
    void nextStatus_cancelAtPending_movesToCancelled() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.CANCEL);
        assertEquals(TicketStatusEnum.CANCELLED.getStatus(), to);
    }

    @Test
    void nextStatus_cancelAtReturned_movesToCancelled() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.RETURNED.getStatus(), TicketActionEnum.CANCEL);
        assertEquals(TicketStatusEnum.CANCELLED.getStatus(), to);
    }

    @Test
    void nextStatus_transferAtReturned_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(
                TicketStatusEnum.RETURNED.getStatus(), TicketActionEnum.TRANSFER));
    }

    @Test
    void nextStatus_transferAtNonTerminal_keepsStatus() {
        for (TicketStatusEnum from : new TicketStatusEnum[]{
                TicketStatusEnum.PENDING, TicketStatusEnum.IN_PROGRESS, TicketStatusEnum.PENDING_REVIEW}) {
            Integer to = TicketStateMachine.nextStatus(from.getStatus(), TicketActionEnum.TRANSFER);
            assertEquals(from.getStatus(), to, "transfer should keep status for " + from);
        }
    }

    @Test
    void nextStatus_finishAtPending_returnsNull() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.FINISH);
        assertNull(to);
    }

    @Test
    void nextStatus_cancelAtClosed_returnsNull() {
        Integer to = TicketStateMachine.nextStatus(TicketStatusEnum.CLOSED.getStatus(), TicketActionEnum.CANCEL);
        assertNull(to);
    }

    @Test
    void nextStatus_transferAtTerminal_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(TicketStatusEnum.COMPLETED.getStatus(), TicketActionEnum.TRANSFER));
        assertNull(TicketStateMachine.nextStatus(TicketStatusEnum.CLOSED.getStatus(), TicketActionEnum.TRANSFER));
        assertNull(TicketStateMachine.nextStatus(TicketStatusEnum.CANCELLED.getStatus(), TicketActionEnum.TRANSFER));
    }

    @Test
    void nextStatus_nullInputs_returnsNull() {
        assertNull(TicketStateMachine.nextStatus(null, TicketActionEnum.START));
        assertNull(TicketStateMachine.nextStatus(TicketStatusEnum.PENDING.getStatus(), null));
    }

    @Test
    void checkTransition_legal_returnsTargetStatus() {
        Integer to = TicketStateMachine.checkTransition(
                TicketStatusEnum.IN_PROGRESS.getStatus(), TicketActionEnum.FINISH);
        assertEquals(TicketStatusEnum.PENDING_REVIEW.getStatus(), to);
    }

    @Test
    void checkTransition_illegal_throwsServiceException() {
        ServiceException ex = assertThrows(ServiceException.class, () -> TicketStateMachine.checkTransition(
                TicketStatusEnum.COMPLETED.getStatus(), TicketActionEnum.START));
        assertEquals(TICKET_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void evaluate_legal_returnsAllowedResult() {
        TicketStateMachine.Result r = TicketStateMachine.evaluate(
                TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.START);
        assertTrue(r.isAllowed());
        assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), r.getToStatus());
        assertNull(r.getReason());
    }

    @Test
    void evaluate_illegal_returnsBlockedResultWithReason() {
        TicketStateMachine.Result r = TicketStateMachine.evaluate(
                TicketStatusEnum.PENDING.getStatus(), TicketActionEnum.FINISH);
        assertTrue(!r.isAllowed());
        assertNull(r.getToStatus());
        assertTrue(r.getReason() != null && r.getReason().contains("待处理"));
    }
}
