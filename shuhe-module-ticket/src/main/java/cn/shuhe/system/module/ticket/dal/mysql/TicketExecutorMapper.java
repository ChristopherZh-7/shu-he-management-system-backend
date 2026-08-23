package cn.shuhe.system.module.ticket.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketExecutorDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 工单执行人 Mapper（{@code shuhe_ticket_executor}）。
 */
@Mapper
public interface TicketExecutorMapper extends BaseMapperX<TicketExecutorDO> {

    /**
     * 按工单 ID 列出所有执行人（未删除）。按 id 升序，便于审计顺序。
     */
    default List<TicketExecutorDO> selectListByTicketId(Long ticketId) {
        return selectList(new LambdaQueryWrapperX<TicketExecutorDO>()
                .eq(TicketExecutorDO::getTicketId, ticketId)
                .orderByAsc(TicketExecutorDO::getId));
    }

    /**
     * 批量按工单 ID 集合查执行人（详情/列表批量展示用）。
     */
    default List<TicketExecutorDO> selectListByTicketIds(Collection<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<TicketExecutorDO>()
                .in(TicketExecutorDO::getTicketId, ticketIds));
    }

    /**
     * 统计工单执行人数。
     */
    default Long selectCountByTicketId(Long ticketId) {
        return selectCount(new LambdaQueryWrapperX<TicketExecutorDO>()
                .eq(TicketExecutorDO::getTicketId, ticketId));
    }

    default boolean existsByTicketIdAndUserId(Long ticketId, Long userId) {
        if (ticketId == null || userId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<TicketExecutorDO>()
                .eq(TicketExecutorDO::getTicketId, ticketId)
                .eq(TicketExecutorDO::getUserId, userId)) > 0;
    }

    /**
     * 同步一张工单的全部执行人状态。工单级「提交交付」视为整个执行组已完成；
     * 驳回或重开后则整组恢复为执行中。
     */
    default void updateStatusByTicketId(Long ticketId, Integer status) {
        update(null, new LambdaUpdateWrapper<TicketExecutorDO>()
                .eq(TicketExecutorDO::getTicketId, ticketId)
                .set(TicketExecutorDO::getStatus, status));
    }

    /**
     * 查某用户作为执行人的全部工单 ID（去重；「与我相关 / 待我办理」列表用）。
     */
    default List<Long> selectTicketIdsByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<TicketExecutorDO>()
                .eq(TicketExecutorDO::getUserId, userId))
                .stream()
                .map(TicketExecutorDO::getTicketId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

}
