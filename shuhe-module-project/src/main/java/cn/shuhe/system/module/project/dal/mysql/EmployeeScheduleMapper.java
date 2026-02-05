package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.EmployeeScheduleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工工作排期 Mapper
 */
@Mapper
public interface EmployeeScheduleMapper extends BaseMapperX<EmployeeScheduleDO> {

    /**
     * 根据员工ID查询排期列表
     */
    default List<EmployeeScheduleDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getUserId, userId)
                .orderByAsc(EmployeeScheduleDO::getPlanStartTime));
    }

    /**
     * 根据员工ID查询进行中的排期
     */
    default List<EmployeeScheduleDO> selectInProgressByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getUserId, userId)
                .eq(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_IN_PROGRESS)
                .orderByAsc(EmployeeScheduleDO::getPlanStartTime));
    }

    /**
     * 根据员工ID查询排队中的排期
     */
    default List<EmployeeScheduleDO> selectQueuedByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getUserId, userId)
                .eq(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_QUEUED)
                .orderByAsc(EmployeeScheduleDO::getQueueOrder));
    }

    /**
     * 根据部门ID查询所有员工的排期
     */
    default List<EmployeeScheduleDO> selectListByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getDeptId, deptId)
                .in(EmployeeScheduleDO::getStatus, 
                        EmployeeScheduleDO.STATUS_QUEUED, 
                        EmployeeScheduleDO.STATUS_IN_PROGRESS)
                .orderByAsc(EmployeeScheduleDO::getUserId)
                .orderByAsc(EmployeeScheduleDO::getPlanStartTime));
    }

    /**
     * 根据部门ID查询进行中的排期
     */
    default List<EmployeeScheduleDO> selectInProgressByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getDeptId, deptId)
                .eq(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_IN_PROGRESS)
                .orderByAsc(EmployeeScheduleDO::getUserId)
                .orderByAsc(EmployeeScheduleDO::getPlanStartTime));
    }

    /**
     * 根据部门ID查询排队中的排期
     */
    default List<EmployeeScheduleDO> selectQueuedByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getDeptId, deptId)
                .eq(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_QUEUED)
                .orderByAsc(EmployeeScheduleDO::getQueueOrder));
    }

    /**
     * 根据服务申请ID查询排期
     */
    default EmployeeScheduleDO selectByLaunchId(Long launchId) {
        return selectOne(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getLaunchId, launchId));
    }

    /**
     * 根据轮次ID查询排期
     */
    default EmployeeScheduleDO selectByRoundId(Long roundId) {
        return selectOne(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getRoundId, roundId));
    }

    /**
     * 查询员工在指定时间段内是否有安排
     */
    default List<EmployeeScheduleDO> selectConflicts(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getUserId, userId)
                .in(EmployeeScheduleDO::getStatus, 
                        EmployeeScheduleDO.STATUS_QUEUED, 
                        EmployeeScheduleDO.STATUS_IN_PROGRESS)
                // 时间段有重叠
                .lt(EmployeeScheduleDO::getPlanStartTime, endTime)
                .gt(EmployeeScheduleDO::getPlanEndTime, startTime));
    }

    /**
     * 获取部门当前最大排队顺序
     */
    default Integer selectMaxQueueOrderByDeptId(Long deptId) {
        EmployeeScheduleDO schedule = selectOne(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getDeptId, deptId)
                .eq(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_QUEUED)
                .orderByDesc(EmployeeScheduleDO::getQueueOrder)
                .last("LIMIT 1"));
        return schedule != null ? schedule.getQueueOrder() : 0;
    }

    /**
     * 根据服务申请ID删除排期
     */
    default int deleteByLaunchId(Long launchId) {
        return delete(new LambdaQueryWrapperX<EmployeeScheduleDO>()
                .eq(EmployeeScheduleDO::getLaunchId, launchId));
    }

}
