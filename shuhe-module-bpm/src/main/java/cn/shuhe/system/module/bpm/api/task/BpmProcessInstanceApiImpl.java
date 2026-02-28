package cn.shuhe.system.module.bpm.api.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.shuhe.system.module.bpm.service.task.BpmProcessInstanceService;
import cn.shuhe.system.module.bpm.service.task.BpmTaskService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author ShuHe
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private BpmTaskService taskService;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public boolean isTaskAssignee(Long userId, String processInstanceId) {
        if (userId == null || StrUtil.isEmpty(processInstanceId)) {
            return false;
        }
        
        // 1. 首先检查待办任务（正在进行中的任务）
        List<Task> runningTasks = taskService.getRunningTaskListByProcessInstanceId(processInstanceId, null, null);
        if (CollUtil.isNotEmpty(runningTasks)) {
            for (Task task : runningTasks) {
                Long assignee = NumberUtil.parseLong(task.getAssignee(), null);
                if (ObjectUtil.equals(userId, assignee)) {
                    return true;
                }
            }
        }
        
        // 2. 检查历史任务（已完成的任务），用于审批完成后仍能查看
        List<HistoricTaskInstance> historicTasks = taskService.getTaskListByProcessInstanceId(processInstanceId, true);
        if (CollUtil.isNotEmpty(historicTasks)) {
            for (HistoricTaskInstance task : historicTasks) {
                Long assignee = NumberUtil.parseLong(task.getAssignee(), null);
                if (ObjectUtil.equals(userId, assignee)) {
                    return true;
                }
            }
        }
        
        return false;
    }

}
