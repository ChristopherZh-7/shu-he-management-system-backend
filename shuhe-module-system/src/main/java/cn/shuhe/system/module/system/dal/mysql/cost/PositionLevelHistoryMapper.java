package cn.shuhe.system.module.system.dal.mysql.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.system.dal.dataobject.cost.PositionLevelHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 职级变更记录 Mapper
 */
@Mapper
public interface PositionLevelHistoryMapper extends BaseMapperX<PositionLevelHistoryDO> {

    /**
     * 根据用户ID查询职级变更记录，按生效日期排序
     */
    default List<PositionLevelHistoryDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                .eq(PositionLevelHistoryDO::getUserId, userId)
                .orderByAsc(PositionLevelHistoryDO::getEffectiveDate));
    }

    /**
     * 查询用户在指定日期范围内的职级变更记录
     */
    default List<PositionLevelHistoryDO> selectByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                .eq(PositionLevelHistoryDO::getUserId, userId)
                .ge(PositionLevelHistoryDO::getEffectiveDate, startDate)
                .le(PositionLevelHistoryDO::getEffectiveDate, endDate)
                .orderByAsc(PositionLevelHistoryDO::getEffectiveDate));
    }

    /**
     * 查询用户在指定年份的职级变更记录
     */
    default List<PositionLevelHistoryDO> selectByUserIdAndYear(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return selectByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 查询用户最新的职级记录（生效日期最晚的）
     */
    default PositionLevelHistoryDO selectLatestByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                .eq(PositionLevelHistoryDO::getUserId, userId)
                .orderByDesc(PositionLevelHistoryDO::getEffectiveDate)
                .last("LIMIT 1"));
    }

    /**
     * 分页查询职级变更记录
     */
    default PageResult<PositionLevelHistoryDO> selectPage(Long userId, String nickname, Integer changeType,
                                                          LocalDate startDate, LocalDate endDate,
                                                          Integer pageNo, Integer pageSize) {
        return selectPage(pageNo, pageSize, new LambdaQueryWrapperX<PositionLevelHistoryDO>()
                .eqIfPresent(PositionLevelHistoryDO::getUserId, userId)
                .eqIfPresent(PositionLevelHistoryDO::getChangeType, changeType)
                .geIfPresent(PositionLevelHistoryDO::getEffectiveDate, startDate)
                .leIfPresent(PositionLevelHistoryDO::getEffectiveDate, endDate)
                .orderByDesc(PositionLevelHistoryDO::getEffectiveDate));
    }

    /**
     * 分页查询
     */
    default PageResult<PositionLevelHistoryDO> selectPage(Integer pageNo, Integer pageSize,
                                                          LambdaQueryWrapperX<PositionLevelHistoryDO> wrapper) {
        return selectPage(new cn.shuhe.system.framework.common.pojo.PageParam()
                .setPageNo(pageNo).setPageSize(pageSize), wrapper);
    }

}
