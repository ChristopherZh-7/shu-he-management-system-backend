package cn.shuhe.system.module.bpm.api.dashboard;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.date.DateUtils;
import cn.shuhe.system.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.shuhe.system.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.shuhe.system.module.bpm.service.task.BpmTaskService;
import cn.shuhe.system.module.system.api.dashboard.DashboardBpmApi;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 仪表板 - BPM 待办/任务统计 API 实现
 * 为工作台/分析页提供真实待办与任务数据
 */
@Service
public class DashboardBpmApiImpl implements DashboardBpmApi {

    @Resource
    private BpmTaskService bpmTaskService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;

    @Override
    public DashboardStatisticsRespVO.TaskStats getTaskStats(Long userId) {
        BpmTaskPageReqVO todoReq = new BpmTaskPageReqVO();
        todoReq.setPageSize(1);
        todoReq.setPageNo(1);
        PageResult<Task> todoPage = bpmTaskService.getTaskTodoPage(userId, todoReq);
        int todoCount = todoPage.getTotal() != null ? todoPage.getTotal().intValue() : 0;

        java.time.LocalDateTime todayStart = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.LocalDateTime weekStart = todayStart.minusDays(todayStart.getDayOfWeek().getValue() - 1);
        long todayDoneCount = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskAssignee(String.valueOf(userId))
                .taskCompletedAfter(DateUtils.of(todayStart))
                .count();
        long weeklyDoneCount = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .taskAssignee(String.valueOf(userId))
                .taskCompletedAfter(DateUtils.of(weekStart))
                .count();

        return DashboardStatisticsRespVO.TaskStats.builder()
                .todoCount(todoCount)
                .todayDoneCount((int) todayDoneCount)
                .weeklyDoneCount((int) weeklyDoneCount)
                .overdueCount(0)
                .build();
    }

    @Override
    public List<DashboardStatisticsRespVO.TodoItem> getTodoList(Long userId, int limit) {
        BpmTaskPageReqVO pageVO = new BpmTaskPageReqVO();
        pageVO.setPageSize(limit);
        pageVO.setPageNo(1);
        PageResult<Task> pageResult = bpmTaskService.getTaskTodoPage(userId, pageVO);
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> definitionIds = new java.util.HashSet<>();
        for (Task t : pageResult.getList()) {
            definitionIds.add(t.getProcessDefinitionId());
        }
        java.util.Map<String, String> processNameMap = new java.util.HashMap<>();
        for (String defId : definitionIds) {
            try {
                org.flowable.engine.repository.ProcessDefinition def = processDefinitionService.getProcessDefinition(defId);
                if (def != null && StrUtil.isNotBlank(def.getName())) {
                    processNameMap.put(defId, def.getName());
                }
            } catch (Exception ignored) {
            }
        }
        List<DashboardStatisticsRespVO.TodoItem> list = new ArrayList<>();
        for (Task task : pageResult.getList()) {
            String processName = processNameMap.getOrDefault(task.getProcessDefinitionId(), "流程");
            String createTimeDesc = formatRelativeTime(task.getCreateTime());
            list.add(DashboardStatisticsRespVO.TodoItem.builder()
                    .id(task.getId())
                    .title(StrUtil.isNotBlank(task.getName()) ? task.getName() : processName)
                    .processName(processName)
                    .createTimeDesc(createTimeDesc)
                    .status("pending")
                    .processInstanceId(task.getProcessInstanceId())
                    .build());
        }
        return list;
    }

    @Override
    public List<DashboardStatisticsRespVO.PieChartData> getTaskDistribution(Long userId, boolean isAdmin) {
        return null;
    }

    private static String formatRelativeTime(Date date) {
        if (date == null) return "";
        long diffMs = System.currentTimeMillis() - date.getTime();
        long diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        long diffHours = TimeUnit.MILLISECONDS.toHours(diffMs);
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);
        if (diffMinutes < 60) return diffMinutes <= 0 ? "刚刚" : diffMinutes + "分钟前";
        if (diffHours < 24) return diffHours + "小时前";
        if (diffDays == 0) return "今天 " + new java.text.SimpleDateFormat("HH:mm").format(date);
        if (diffDays == 1) return "昨天 " + new java.text.SimpleDateFormat("HH:mm").format(date);
        if (diffDays < 7) return diffDays + "天前";
        return new java.text.SimpleDateFormat("MM-dd HH:mm").format(date);
    }
}
