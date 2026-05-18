package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCommentSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCommentDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 工单评论 Service 接口（{@code shuhe_ticket_comment}）。
 */
public interface TicketCommentService {

    Long createComment(@Valid TicketCommentSaveReqVO reqVO);

    /**
     * 列出指定工单的评论，按当前用户身份过滤内部评论（提单人看不到）。
     */
    List<TicketCommentDO> listByTicket(Long ticketId, Long currentUserId);

    /** 统计工单评论数（用于详情页）。 */
    Long countByTicket(Long ticketId);

}
