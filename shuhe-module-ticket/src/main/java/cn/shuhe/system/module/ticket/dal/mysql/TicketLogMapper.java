package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工单操作日志 Mapper（{@code shuhe_ticket_log}）。
 */
@Mapper
public interface TicketLogMapper extends BaseMapperX<TicketLogDO> {

    /**
     * 按工单 ID 时间倒序拉日志（用于详情页时间线展示）。
     */
    default List<TicketLogDO> selectListByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapperX<TicketLogDO>()
                .eq(TicketLogDO::getTicketId, ticketId)
                .orderByDesc(TicketLogDO::getId));
    }

}
