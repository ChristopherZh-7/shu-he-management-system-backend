package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.project.dal.dataobject.EmployeeScheduleDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.EmployeeScheduleMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工工作排期 Service 实现
 * 
 * 基于 project_round 表查询员工工作状态
 */
@Service
@Validated
@Slf4j
public class EmployeeScheduleServiceImpl implements EmployeeScheduleService {

    @Resource
    private EmployeeScheduleMapper employeeScheduleMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    // ==================== 员工空置查询（基于 project_round）====================

    @Override
    public List<Map<String, Object>> getDeptEmployeeStatus(Long deptId) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (deptId == null) {
            return result;
        }

        // 1. 获取部门及子部门的所有用户
        Set<Long> allDeptIds = new HashSet<>();
        allDeptIds.add(deptId);
        List<DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
        if (childDepts != null) {
            for (DeptRespDTO childDept : childDepts) {
                allDeptIds.add(childDept.getId());
            }
        }

        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(allDeptIds);
        if (CollUtil.isEmpty(users)) {
            return result;
        }

        // 2. 获取所有进行中的轮次（status = 0待执行 或 1执行中）
        List<ProjectRoundDO> activeRounds = projectRoundMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectRoundDO>()
                        .in(ProjectRoundDO::getStatus, 0, 1)
        );

        // 3. 解析每个轮次的执行人，构建 userId -> 轮次列表 的映射
        Map<Long, List<ProjectRoundDO>> roundsByUser = new HashMap<>();
        for (ProjectRoundDO round : activeRounds) {
            List<Long> executorIds = parseExecutorIds(round.getExecutorIds());
            for (Long executorId : executorIds) {
                roundsByUser.computeIfAbsent(executorId, k -> new ArrayList<>()).add(round);
            }
        }

        // 4. 构建每个用户的状态
        for (AdminUserRespDTO user : users) {
            Map<String, Object> userStatus = new HashMap<>();
            userStatus.put("userId", user.getId());
            userStatus.put("userName", user.getNickname());
            userStatus.put("deptId", user.getDeptId());

            List<ProjectRoundDO> userRounds = roundsByUser.getOrDefault(user.getId(), Collections.emptyList());
            
            // 检查执行中的轮次（status = 1）
            List<ProjectRoundDO> inProgressRounds = userRounds.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                    .collect(Collectors.toList());
            
            // 检查待执行的轮次（status = 0）
            List<ProjectRoundDO> pendingRounds = userRounds.stream()
                    .filter(r -> r.getStatus() != null && r.getStatus() == 0)
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(inProgressRounds)) {
                // 有执行中的轮次 = 忙碌
                userStatus.put("status", "busy");
                ProjectRoundDO currentRound = inProgressRounds.get(0);
                
                // 获取服务项名称作为任务描述
                String taskDescription = currentRound.getName();
                if (taskDescription == null && currentRound.getServiceItemId() != null) {
                    ServiceItemDO serviceItem = serviceItemMapper.selectById(currentRound.getServiceItemId());
                    if (serviceItem != null) {
                        taskDescription = serviceItem.getName();
                    }
                }
                
                userStatus.put("currentTask", taskDescription);
                userStatus.put("planEndTime", currentRound.getPlanEndTime());
                userStatus.put("expectedFreeTime", currentRound.getPlanEndTime() != null 
                        ? currentRound.getPlanEndTime() 
                        : currentRound.getDeadline());
            } else if (CollUtil.isNotEmpty(pendingRounds)) {
                // 有待执行的轮次，但当前空闲
                userStatus.put("status", "idle");
                userStatus.put("currentTask", null);
                userStatus.put("expectedFreeTime", null);
            } else {
                // 完全空闲
                userStatus.put("status", "idle");
                userStatus.put("currentTask", null);
                userStatus.put("expectedFreeTime", null);
            }

            // 待执行任务数量（类似排队）
            userStatus.put("queueCount", pendingRounds.size());
            userStatus.put("queuedTasks", pendingRounds.stream()
                    .map(r -> {
                        Map<String, Object> task = new HashMap<>();
                        task.put("id", r.getId());
                        task.put("description", r.getName());
                        task.put("expectedStartTime", r.getDeadline());
                        task.put("queueOrder", r.getRoundNo());
                        return task;
                    })
                    .collect(Collectors.toList()));

            result.add(userStatus);
        }

        // 5. 排序：空闲的排前面
        result.sort((a, b) -> {
            String statusA = (String) a.get("status");
            String statusB = (String) b.get("status");
            if ("idle".equals(statusA) && !"idle".equals(statusB)) return -1;
            if (!"idle".equals(statusA) && "idle".equals(statusB)) return 1;
            return 0;
        });

        log.info("【获取部门员工状态】deptId={}, 共{}名员工", deptId, result.size());
        return result;
    }

    /**
     * 解析执行人ID列表（JSON数组字符串）
     */
    private List<Long> parseExecutorIds(String executorIdsJson) {
        if (executorIdsJson == null || executorIdsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(executorIdsJson, Long.class);
        } catch (Exception e) {
            log.warn("解析执行人ID列表失败: {}", executorIdsJson);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAvailableEmployees(Long deptId) {
        List<Map<String, Object>> allStatus = getDeptEmployeeStatus(deptId);
        return allStatus.stream()
                .filter(s -> "idle".equals(s.get("status")))
                .collect(Collectors.toList());
    }

    @Override
    public LocalDateTime getEarliestAvailableTime(Long deptId) {
        // 获取部门及子部门的所有用户
        Set<Long> allDeptIds = new HashSet<>();
        allDeptIds.add(deptId);
        List<DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
        if (childDepts != null) {
            for (DeptRespDTO childDept : childDepts) {
                allDeptIds.add(childDept.getId());
            }
        }
        
        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(allDeptIds);
        if (CollUtil.isEmpty(users)) {
            return LocalDateTime.now();
        }
        
        Set<Long> userIds = users.stream().map(AdminUserRespDTO::getId).collect(Collectors.toSet());

        // 获取所有执行中的轮次
        List<ProjectRoundDO> inProgressRounds = projectRoundMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectRoundDO>()
                        .eq(ProjectRoundDO::getStatus, 1) // 执行中
        );
        
        if (CollUtil.isEmpty(inProgressRounds)) {
            return LocalDateTime.now();
        }

        // 找到该部门员工正在执行的轮次中最早结束的
        LocalDateTime earliest = null;
        for (ProjectRoundDO round : inProgressRounds) {
            List<Long> executorIds = parseExecutorIds(round.getExecutorIds());
            // 检查是否有该部门的员工
            boolean hasDeptUser = executorIds.stream().anyMatch(userIds::contains);
            if (hasDeptUser) {
                LocalDateTime endTime = round.getPlanEndTime() != null ? round.getPlanEndTime() : round.getDeadline();
                if (endTime != null) {
                    if (earliest == null || endTime.isBefore(earliest)) {
                        earliest = endTime;
                    }
                }
            }
        }

        return earliest != null ? earliest : LocalDateTime.now();
    }

    @Override
    public boolean isEmployeeAvailable(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 查询该员工正在执行的轮次
        List<ProjectRoundDO> activeRounds = projectRoundMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProjectRoundDO>()
                        .in(ProjectRoundDO::getStatus, 0, 1)
        );
        
        for (ProjectRoundDO round : activeRounds) {
            List<Long> executorIds = parseExecutorIds(round.getExecutorIds());
            if (executorIds.contains(userId)) {
                // 检查时间是否冲突
                LocalDateTime roundStart = round.getDeadline(); // 使用 deadline 作为开始时间
                LocalDateTime roundEnd = round.getPlanEndTime();
                
                if (roundStart != null && roundEnd != null) {
                    // 时间段有重叠
                    if (startTime.isBefore(roundEnd) && endTime.isAfter(roundStart)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    // ==================== 排期管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(EmployeeScheduleDO schedule) {
        // 填充用户和部门名称
        if (schedule.getUserId() != null && schedule.getUserName() == null) {
            AdminUserRespDTO user = adminUserApi.getUser(schedule.getUserId());
            if (user != null) {
                schedule.setUserName(user.getNickname());
            }
        }
        if (schedule.getDeptId() != null && schedule.getDeptName() == null) {
            DeptRespDTO dept = deptApi.getDept(schedule.getDeptId());
            if (dept != null) {
                schedule.setDeptName(dept.getName());
            }
        }

        employeeScheduleMapper.insert(schedule);
        log.info("【创建排期】userId={}, status={}, id={}", schedule.getUserId(), schedule.getStatus(), schedule.getId());
        return schedule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQueuedSchedule(Long userId, Long deptId, Long launchId, 
                                      LocalDateTime expectedStartTime, String taskDescription) {
        // 获取当前最大排队顺序
        Integer maxOrder = employeeScheduleMapper.selectMaxQueueOrderByDeptId(deptId);
        int newOrder = (maxOrder != null ? maxOrder : 0) + 1;

        EmployeeScheduleDO schedule = EmployeeScheduleDO.builder()
                .userId(userId)
                .deptId(deptId)
                .launchId(launchId)
                .status(EmployeeScheduleDO.STATUS_QUEUED)
                .expectedStartTime(expectedStartTime)
                .queueOrder(newOrder)
                .taskType(EmployeeScheduleDO.TASK_TYPE_CROSS_DEPT)
                .taskDescription(taskDescription)
                .build();

        return createSchedule(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateSchedule(Long scheduleId, LocalDateTime planStartTime, LocalDateTime planEndTime) {
        EmployeeScheduleDO schedule = employeeScheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            log.warn("【激活排期】排期不存在，id={}", scheduleId);
            return;
        }

        EmployeeScheduleDO update = new EmployeeScheduleDO();
        update.setId(scheduleId);
        update.setStatus(EmployeeScheduleDO.STATUS_IN_PROGRESS);
        update.setPlanStartTime(planStartTime);
        update.setPlanEndTime(planEndTime);
        update.setActualStartTime(LocalDateTime.now());
        update.setQueueOrder(null); // 清除排队顺序

        employeeScheduleMapper.updateById(update);
        log.info("【激活排期】id={}, planStartTime={}, planEndTime={}", scheduleId, planStartTime, planEndTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSchedule(Long scheduleId) {
        EmployeeScheduleDO update = new EmployeeScheduleDO();
        update.setId(scheduleId);
        update.setStatus(EmployeeScheduleDO.STATUS_COMPLETED);
        update.setActualEndTime(LocalDateTime.now());

        employeeScheduleMapper.updateById(update);
        log.info("【完成排期】id={}", scheduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSchedule(Long scheduleId) {
        EmployeeScheduleDO update = new EmployeeScheduleDO();
        update.setId(scheduleId);
        update.setStatus(EmployeeScheduleDO.STATUS_CANCELLED);

        employeeScheduleMapper.updateById(update);
        log.info("【取消排期】id={}", scheduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelScheduleByLaunchId(Long launchId) {
        EmployeeScheduleDO schedule = employeeScheduleMapper.selectByLaunchId(launchId);
        if (schedule != null) {
            cancelSchedule(schedule.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScheduleRoundId(Long launchId, Long roundId) {
        EmployeeScheduleDO schedule = employeeScheduleMapper.selectByLaunchId(launchId);
        if (schedule != null) {
            EmployeeScheduleDO update = new EmployeeScheduleDO();
            update.setId(schedule.getId());
            update.setRoundId(roundId);
            employeeScheduleMapper.updateById(update);
            log.info("【更新排期轮次】scheduleId={}, roundId={}", schedule.getId(), roundId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeScheduleByRoundId(Long roundId) {
        if (roundId == null) {
            return;
        }
        // 查询该轮次关联的所有排期记录
        List<EmployeeScheduleDO> schedules = employeeScheduleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmployeeScheduleDO>()
                        .eq(EmployeeScheduleDO::getRoundId, roundId)
                        .in(EmployeeScheduleDO::getStatus, EmployeeScheduleDO.STATUS_QUEUED, EmployeeScheduleDO.STATUS_IN_PROGRESS)
        );
        
        if (schedules == null || schedules.isEmpty()) {
            log.info("【完成排期】轮次 {} 没有关联的排期记录", roundId);
            return;
        }
        
        for (EmployeeScheduleDO schedule : schedules) {
            completeSchedule(schedule.getId());
        }
        log.info("【完成排期】轮次 {} 关联的 {} 条排期已完成", roundId, schedules.size());
    }

    // ==================== 查询 ====================

    @Override
    public EmployeeScheduleDO getSchedule(Long id) {
        return employeeScheduleMapper.selectById(id);
    }

    @Override
    public EmployeeScheduleDO getScheduleByLaunchId(Long launchId) {
        return employeeScheduleMapper.selectByLaunchId(launchId);
    }

    @Override
    public List<EmployeeScheduleDO> getSchedulesByUserId(Long userId) {
        return employeeScheduleMapper.selectListByUserId(userId);
    }

    @Override
    public List<EmployeeScheduleDO> getSchedulesByDeptId(Long deptId) {
        return employeeScheduleMapper.selectListByDeptId(deptId);
    }

    @Override
    public List<EmployeeScheduleDO> getQueueByDeptId(Long deptId) {
        return employeeScheduleMapper.selectQueuedByDeptId(deptId);
    }

}
