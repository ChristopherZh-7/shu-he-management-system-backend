package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TicketLogMapper extends BaseMapperX<TicketLogDO> {

    default List<TicketLogDO> selectListByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapperX<TicketLogDO>()
                .eq(TicketLogDO::getTicketId, ticketId)
                .orderByDesc(TicketLogDO::getId));
    }

}
