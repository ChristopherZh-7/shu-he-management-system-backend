package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 Mapper（{@code shuhe_ticket}）。
 *
 * <p>分页查询的 {@code selectPage(reqVO)} 方法在批 5（Controller / Service）落地后补，
 * 此处先暴露最小可用方法集，供批 3 / 批 4 Service 使用。
 */
@Mapper
public interface TicketMapper extends BaseMapperX<TicketDO> {

    /**
     * 按工单编号唯一查找。
     */
    default TicketDO selectByTicketNo(String ticketNo) {
        return selectOne(TicketDO::getTicketNo, ticketNo);
    }

    /**
     * 按业务来源查找（避免重复落工单）。
     */
    default TicketDO selectByBusinessTypeAndId(String businessType, Long businessId) {
        return selectOne(new LambdaQueryWrapperX<TicketDO>()
                .eq(TicketDO::getBusinessType, businessType)
                .eq(TicketDO::getBusinessId, businessId));
    }

    /**
     * 统计指定分类下的工单数量（用于分类删除校验）。
     */
    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(new LambdaQueryWrapper<TicketDO>()
                .eq(TicketDO::getCategoryId, categoryId));
    }

    /**
     * 找出当天最新一条工单（用于生成 {@code TKyyyyMMddxxx} 流水号）。
     */
    default TicketDO selectLatestByDay(LocalDateTime dayStart, LocalDateTime dayEnd) {
        return selectOne(new LambdaQueryWrapperX<TicketDO>()
                .ge(TicketDO::getCreateTime, dayStart)
                .lt(TicketDO::getCreateTime, dayEnd)
                .orderByDesc(TicketDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 列出关联到指定业务的所有工单。
     */
    default List<TicketDO> selectListByBusinessTypeAndId(String businessType, Long businessId) {
        return selectList(new LambdaQueryWrapperX<TicketDO>()
                .eq(TicketDO::getBusinessType, businessType)
                .eq(TicketDO::getBusinessId, businessId)
                .orderByDesc(TicketDO::getId));
    }

    default List<TicketDO> selectListByAssigneeId(Long assigneeId) {
        return selectList(new LambdaQueryWrapperX<TicketDO>()
                .eq(TicketDO::getAssigneeId, assigneeId)
                .orderByDesc(TicketDO::getId));
    }

}
