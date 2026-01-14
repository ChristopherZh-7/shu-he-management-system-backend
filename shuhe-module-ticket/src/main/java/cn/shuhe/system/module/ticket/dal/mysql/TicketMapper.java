package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapperX<TicketDO> {

    default PageResult<TicketDO> selectPage(TicketPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TicketDO>()
                .likeIfPresent(TicketDO::getTicketNo, reqVO.getTicketNo())
                .likeIfPresent(TicketDO::getTitle, reqVO.getTitle())
                .eqIfPresent(TicketDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(TicketDO::getPriority, reqVO.getPriority())
                .eqIfPresent(TicketDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TicketDO::getAssigneeId, reqVO.getAssigneeId())
                .eqIfPresent(TicketDO::getCreatorId, reqVO.getCreatorId())
                .betweenIfPresent(TicketDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TicketDO::getId));
    }

    default TicketDO selectByTicketNo(String ticketNo) {
        return selectOne(TicketDO::getTicketNo, ticketNo);
    }

    /**
     * 查询即将到期的工单
     * 条件：期望完成时间在指定范围内，状态为待处理/已分配/处理中（0/1/2）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 即将到期的工单列表
     */
    default List<TicketDO> selectExpiringTickets(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<TicketDO>()
                .isNotNull(TicketDO::getExpectTime)
                .ge(TicketDO::getExpectTime, startTime)
                .le(TicketDO::getExpectTime, endTime)
                .in(TicketDO::getStatus, 0, 1, 2)); // 待处理、已分配、处理中
    }

}
