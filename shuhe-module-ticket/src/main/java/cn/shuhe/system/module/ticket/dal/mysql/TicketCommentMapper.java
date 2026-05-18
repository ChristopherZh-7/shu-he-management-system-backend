package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCommentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工单评论 Mapper（{@code shuhe_ticket_comment}）。
 */
@Mapper
public interface TicketCommentMapper extends BaseMapperX<TicketCommentDO> {

    /**
     * 按工单 ID 列出全部评论（包含内部评论；过滤由 Service 层做）。
     */
    default List<TicketCommentDO> selectListByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapperX<TicketCommentDO>()
                .eq(TicketCommentDO::getTicketId, ticketId)
                .orderByAsc(TicketCommentDO::getId));
    }

    /**
     * 统计工单评论数（详情页用）。
     */
    default Long selectCountByTicketId(Long ticketId) {
        return selectCount(new LambdaQueryWrapperX<TicketCommentDO>()
                .eq(TicketCommentDO::getTicketId, ticketId));
    }

}
