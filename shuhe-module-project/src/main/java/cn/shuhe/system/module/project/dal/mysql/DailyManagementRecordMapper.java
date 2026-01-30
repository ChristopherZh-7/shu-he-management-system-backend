package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.DailyManagementRecordPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 日常管理记录 Mapper
 */
@Mapper
public interface DailyManagementRecordMapper extends BaseMapperX<DailyManagementRecordDO> {

    /**
     * 分页查询日常管理记录
     */
    default PageResult<DailyManagementRecordDO> selectPage(DailyManagementRecordPageReqVO reqVO, Collection<Long> deptIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DailyManagementRecordDO>()
                .eqIfPresent(DailyManagementRecordDO::getYear, reqVO.getYear())
                .eqIfPresent(DailyManagementRecordDO::getWeekNumber, reqVO.getWeekNumber())
                .eqIfPresent(DailyManagementRecordDO::getCreator, reqVO.getCreatorId() != null ? String.valueOf(reqVO.getCreatorId()) : null)
                .inIfPresent(DailyManagementRecordDO::getDeptId, deptIds)
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isEmpty(), wrapper ->
                        wrapper.like(DailyManagementRecordDO::getMondayContent, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getTuesdayContent, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getWednesdayContent, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getThursdayContent, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getFridayContent, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getWeeklySummary, reqVO.getKeyword())
                                .or().like(DailyManagementRecordDO::getCreatorName, reqVO.getKeyword())
                )
                .orderByDesc(DailyManagementRecordDO::getYear)
                .orderByDesc(DailyManagementRecordDO::getWeekNumber));
    }

    /**
     * 查询某人某年某周的记录
     */
    default DailyManagementRecordDO selectByCreatorAndYearAndWeek(String creatorId, Integer year, Integer weekNumber) {
        return selectOne(new LambdaQueryWrapperX<DailyManagementRecordDO>()
                .eq(DailyManagementRecordDO::getCreator, creatorId)
                .eq(DailyManagementRecordDO::getYear, year)
                .eq(DailyManagementRecordDO::getWeekNumber, weekNumber));
    }

    /**
     * 查询某人的所有记录
     */
    default List<DailyManagementRecordDO> selectListByCreator(String creatorId) {
        return selectList(new LambdaQueryWrapperX<DailyManagementRecordDO>()
                .eq(DailyManagementRecordDO::getCreator, creatorId)
                .orderByDesc(DailyManagementRecordDO::getYear)
                .orderByDesc(DailyManagementRecordDO::getWeekNumber));
    }

    /**
     * 查询某年某周的所有记录
     */
    default List<DailyManagementRecordDO> selectListByYearAndWeek(Integer year, Integer weekNumber) {
        return selectList(new LambdaQueryWrapperX<DailyManagementRecordDO>()
                .eq(DailyManagementRecordDO::getYear, year)
                .eq(DailyManagementRecordDO::getWeekNumber, weekNumber)
                .orderByAsc(DailyManagementRecordDO::getDeptId));
    }

}
