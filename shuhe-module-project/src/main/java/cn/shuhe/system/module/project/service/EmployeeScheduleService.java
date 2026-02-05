package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.dal.dataobject.EmployeeScheduleDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 员工工作排期 Service
 */
public interface EmployeeScheduleService {

    // ==================== 员工空置查询 ====================

    /**
     * 获取部门员工空置情况
     * 
     * @param deptId 部门ID
     * @return 员工状态列表，包含：
     *         - userId: 员工ID
     *         - userName: 员工姓名
     *         - status: 状态（idle=空闲, busy=忙碌, queued=有排队任务）
     *         - currentTask: 当前任务描述（忙碌时）
     *         - expectedFreeTime: 预计空闲时间（忙碌时）
     *         - queueCount: 排队任务数量
     */
    List<Map<String, Object>> getDeptEmployeeStatus(Long deptId);

    /**
     * 获取部门空闲员工列表
     * 
     * @param deptId 部门ID
     * @return 空闲员工列表
     */
    List<Map<String, Object>> getAvailableEmployees(Long deptId);

    /**
     * 获取部门最早可安排时间
     * 
     * @param deptId 部门ID
     * @return 最早可安排时间
     */
    LocalDateTime getEarliestAvailableTime(Long deptId);

    /**
     * 检查员工在指定时间段是否可用
     * 
     * @param userId 员工ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return true=可用，false=有冲突
     */
    boolean isEmployeeAvailable(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    // ==================== 排期管理 ====================

    /**
     * 创建排期记录
     * 
     * @param schedule 排期信息
     * @return 排期ID
     */
    Long createSchedule(EmployeeScheduleDO schedule);

    /**
     * 创建排队记录
     * 
     * @param userId 员工ID
     * @param deptId 部门ID
     * @param launchId 服务申请ID
     * @param expectedStartTime 期望开始时间
     * @param taskDescription 任务描述
     * @return 排期ID
     */
    Long createQueuedSchedule(Long userId, Long deptId, Long launchId, 
                               LocalDateTime expectedStartTime, String taskDescription);

    /**
     * 将排队中的排期转为进行中
     * 
     * @param scheduleId 排期ID
     * @param planStartTime 计划开始时间
     * @param planEndTime 计划结束时间
     */
    void activateSchedule(Long scheduleId, LocalDateTime planStartTime, LocalDateTime planEndTime);

    /**
     * 完成排期
     * 
     * @param scheduleId 排期ID
     */
    void completeSchedule(Long scheduleId);

    /**
     * 取消排期
     * 
     * @param scheduleId 排期ID
     */
    void cancelSchedule(Long scheduleId);

    /**
     * 根据服务申请ID取消排期
     * 
     * @param launchId 服务申请ID
     */
    void cancelScheduleByLaunchId(Long launchId);

    /**
     * 根据轮次ID更新排期
     * 
     * @param launchId 服务申请ID
     * @param roundId 轮次ID
     */
    void updateScheduleRoundId(Long launchId, Long roundId);

    /**
     * 根据轮次ID完成排期
     * 
     * @param roundId 轮次ID
     */
    void completeScheduleByRoundId(Long roundId);

    // ==================== 查询 ====================

    /**
     * 根据ID获取排期
     */
    EmployeeScheduleDO getSchedule(Long id);

    /**
     * 根据服务申请ID获取排期
     */
    EmployeeScheduleDO getScheduleByLaunchId(Long launchId);

    /**
     * 获取员工的排期列表
     */
    List<EmployeeScheduleDO> getSchedulesByUserId(Long userId);

    /**
     * 获取部门的排期列表
     */
    List<EmployeeScheduleDO> getSchedulesByDeptId(Long deptId);

    /**
     * 获取部门排队列表
     */
    List<EmployeeScheduleDO> getQueueByDeptId(Long deptId);

}
