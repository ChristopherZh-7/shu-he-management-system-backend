package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReopenReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReturnReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewPassReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewRejectReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketTransferReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketExecutorDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContext;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 工单 Service 接口（{@code shuhe_ticket}）
 *
 * <p>覆盖 CRUD + 状态机；权限校验通过 Controller 层 {@code @PreAuthorize} + 本接口 IDOR 校验配合。
 */
public interface TicketService {

    // ========== CRUD ==========

    Long createTicket(@Valid TicketSaveReqVO createReqVO);

    void updateTicket(@Valid TicketSaveReqVO updateReqVO);

    void deleteTicket(Long id);

    TicketDO getTicket(Long id);

    PageResult<TicketDO> getTicketPage(TicketPageReqVO pageReqVO);

    /**
     * 我的工单：当前用户提的 + 处理的；本方法实现内会跳过 {@code @DataPermission}。
     */
    PageResult<TicketDO> getMyTicketPage(TicketPageReqVO pageReqVO, Long userId);

    /** 当前用户可申请工单的精确服务项。 */
    List<TicketServiceContext> getEligibleServiceItems(Long userId, Long projectId);

    // ========== 状态机 ==========

    /** 分派处理人；status: 0 → 0（仅改 assignee）。 */
    void assignTicket(@Valid TicketAssignReqVO reqVO);

    /**
     * 主管接单：写多个执行人、状态 0 → 1、回写 assignee_id、发 {@link
     * cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent} 触发业务驱动器。
     *
     * <p>权限要求：当前用户必须是工单 {@code dept_id} 部门负责人（或 super_admin）。
     */
    void acceptTicket(@Valid TicketAcceptReqVO reqVO);

    /** 接单开始；status: 0 → 1。 */
    void startTicket(Long id);

    /** 完成工单（提交验收）；status: 1 → 2。 */
    void finishTicket(@Valid TicketFinishReqVO reqVO);

    /**
     * 验收通过；status: 2 → 3，回写 reviewer / reviewTime / finishTime。
     *
     * <p>权限要求：提单人本人（或 super_admin）。
     */
    void reviewPassTicket(@Valid TicketReviewPassReqVO reqVO);

    /**
     * 验收驳回；status: 2 → 1，退回原执行人重做，dueTime 不重置。
     *
     * <p>权限要求：提单人本人（或 super_admin）。
     */
    void reviewRejectTicket(@Valid TicketReviewRejectReqVO reqVO);

    /** 关闭工单；status: 3 → 4。 */
    void closeTicket(Long id);

    /**
     * 重开工单；status: 3/4 → 1，保留原执行人，清空 finish/close/review 时间。
     *
     * <p>限制：提单人本人（或 super_admin）；finishTime/closeTime 起 {@code REOPEN_WINDOW_DAYS}
     * 天内；累计不超过 {@code REOPEN_MAX_COUNT} 次。
     */
    void reopenTicket(@Valid TicketReopenReqVO reqVO);

    /**
     * 拒单退回；status: 0 → 6，回写 returnReason。
     *
     * <p>权限要求：工单 {@code dept_id} 部门负责人（或 super_admin）。
     */
    void returnTicket(@Valid TicketReturnReqVO reqVO);

    /**
     * 重新提交；status: 6 → 0，清空 returnReason。
     *
     * <p>权限要求：提单人本人（或 super_admin）。
     */
    void resubmitTicket(Long id);

    /** 取消工单；status: 0 → 5 或 6 → 5。 */
    void cancelTicket(Long id);

    /** 转交工单；状态不变，仅改 assignee_id。 */
    void transferTicket(@Valid TicketTransferReqVO reqVO);

    // ========== 详情扩展 ==========

    /** 列出工单的操作日志（详情页时间线）。 */
    List<TicketLogDO> getTicketLogs(Long ticketId);

    /** 列出工单的执行人记录（详情页 / 列表卡片用）。 */
    List<TicketExecutorDO> getTicketExecutors(Long ticketId);

    /**
     * 计算当前用户对指定工单可执行的 action 列表（按钮显示用）。
     *
     * <p>仅做粗粒度判定：基于状态机 + 用户与工单的关系（提单人 / 处理人 / 部门负责人 / 管理员），
     * 不查权限表 —— 那一层由 Controller 的 {@code @PreAuthorize} 兜底。
     */
    List<String> calculateAvailableActions(TicketDO ticket, Long currentUserId);

    /** 校验工单存在且当前用户有权访问（IDOR 防护）；无权时抛 {@code TICKET_NO_PERMISSION}。 */
    TicketDO validateTicketAccess(Long ticketId, Long currentUserId);

}
