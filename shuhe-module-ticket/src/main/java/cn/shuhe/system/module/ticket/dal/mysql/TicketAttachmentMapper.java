package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工单附件 Mapper（{@code shuhe_ticket_attachment}）。
 */
@Mapper
public interface TicketAttachmentMapper extends BaseMapperX<TicketAttachmentDO> {

    /**
     * 按工单 ID 列出全部附件。
     */
    default List<TicketAttachmentDO> selectListByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapperX<TicketAttachmentDO>()
                .eq(TicketAttachmentDO::getTicketId, ticketId)
                .orderByDesc(TicketAttachmentDO::getId));
    }

    /**
     * 统计工单附件数。
     */
    default Long selectCountByTicketId(Long ticketId) {
        return selectCount(new LambdaQueryWrapperX<TicketAttachmentDO>()
                .eq(TicketAttachmentDO::getTicketId, ticketId));
    }

}
