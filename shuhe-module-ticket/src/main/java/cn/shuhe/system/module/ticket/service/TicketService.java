package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketTransferReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketExecutorDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
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

    /** 完成工单；status: 1 → 3。 */
    void finishTicket(@Valid TicketFinishReqVO reqVO);

    /** 关闭工单；status: 3 → 4。 */
    void closeTicket(Long id);

    /** 取消工单（一期仅允许 status=0）；status: 0 → 5。 */
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
