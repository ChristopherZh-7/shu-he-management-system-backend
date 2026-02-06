package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目轮次 Mapper
 */
@Mapper
public interface ProjectRoundMapper extends BaseMapperX<ProjectRoundDO> {

    /**
     * 根据项目ID查询轮次列表
     */
    default List<ProjectRoundDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId)
                .orderByAsc(ProjectRoundDO::getRoundNo));
    }

    /**
     * 获取项目的最大轮次序号
     */
    default Integer selectMaxRoundNo(Long projectId) {
        ProjectRoundDO round = selectOne(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId)
                .orderByDesc(ProjectRoundDO::getRoundNo)
                .last("LIMIT 1"));
        return round != null ? round.getRoundNo() : 0;
    }

    /**
     * 统计项目的轮次数量
     */
    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId));
    }

    /**
     * 根据服务项ID查询轮次列表
     */
    default List<ProjectRoundDO> selectListByServiceItemId(Long serviceItemId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getServiceItemId, serviceItemId)
                .orderByAsc(ProjectRoundDO::getRoundNo));
    }

    /**
     * 获取服务项的最大轮次序号
     */
    default Integer selectMaxRoundNoByServiceItemId(Long serviceItemId) {
        ProjectRoundDO round = selectOne(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getServiceItemId, serviceItemId)
                .orderByDesc(ProjectRoundDO::getRoundNo)
                .last("LIMIT 1"));
        return round != null ? round.getRoundNo() : 0;
    }

    /**
     * 统计服务项的轮次数量
     */
    default int selectCountByServiceItemId(Long serviceItemId) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getServiceItemId, serviceItemId)));
    }

    /**
     * 统计服务项在指定时间周期内的轮次数量
     * 
     * @param serviceItemId 服务项ID
     * @param periodStart 周期开始时间
     * @param periodEnd 周期结束时间
     * @return 轮次数量
     */
    default int selectCountByServiceItemIdAndPeriod(Long serviceItemId, 
            LocalDateTime periodStart, LocalDateTime periodEnd) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getServiceItemId, serviceItemId)
                .ge(ProjectRoundDO::getCreateTime, periodStart)
                .lt(ProjectRoundDO::getCreateTime, periodEnd)));
    }

    /**
     * 统计有效轮次数量（待执行0 + 执行中1 + 已完成2，不包括已取消3）
     *
     * @param projectId 服务项ID（历史原因用 projectId 命名）
     * @return 有效轮次数量
     */
    default Long selectValidRoundCount(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId)
                .in(ProjectRoundDO::getStatus, 0, 1, 2)); // 待执行、执行中、已完成
    }

    /**
     * 统计服务项已开始执行的轮次数量（执行中1 + 已完成2）
     *
     * @param serviceItemId 服务项ID
     * @return 已执行轮次数量
     */
    default int selectStartedCountByServiceItemId(Long serviceItemId) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getServiceItemId, serviceItemId)
                .in(ProjectRoundDO::getStatus, 1, 2))); // 执行中、已完成
    }

    /**
     * 查询需要提醒的执行计划
     * 条件：
     * 1. remind_time <= 当前时间（到了提醒时间）
     * 2. reminded = 0（未提醒）
     * 3. status IN (0, 1)（待执行或执行中，不包括已完成和已取消）
     *
     * @param now 当前时间
     * @return 需要提醒的执行计划列表
     */
    default List<ProjectRoundDO> selectRoundsNeedRemind(LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundDO>()
                .le(ProjectRoundDO::getRemindTime, now)
                .eq(ProjectRoundDO::getReminded, false)
                .in(ProjectRoundDO::getStatus, 0, 1) // 待执行、执行中
                .isNotNull(ProjectRoundDO::getDeadline)
                .orderByAsc(ProjectRoundDO::getRemindTime));
    }

    /**
     * 查询即将到期的执行计划（用于Dashboard展示）
     * 条件：
     * 1. deadline 在指定天数内
     * 2. status IN (0, 1)（待执行或执行中）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 即将到期的执行计划列表
     */
    default List<ProjectRoundDO> selectUpcomingDeadlineRounds(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundDO>()
                .ge(ProjectRoundDO::getDeadline, startTime)
                .le(ProjectRoundDO::getDeadline, endTime)
                .in(ProjectRoundDO::getStatus, 0, 1) // 待执行、执行中
                .orderByAsc(ProjectRoundDO::getDeadline));
    }

}
