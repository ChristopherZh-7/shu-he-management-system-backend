package cn.shuhe.system.module.project.service.listener;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.shuhe.system.module.project.dal.dataobject.ServiceExecutionDO;
import cn.shuhe.system.module.project.service.ServiceExecutionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务执行发起审批结果的监听器
 * 
 * 当审批通过后，自动创建执行轮次（带执行人信息）
 */
@Component
@Slf4j
public class ServiceExecutionStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 服务执行发起的流程定义 Key
     */
    public static final String PROCESS_DEFINITION_KEY = "service_execution_start";

    @Resource
    private ServiceExecutionService serviceExecutionService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("【服务执行监听】收到流程状态变更事件，processInstanceId={}, status={}",
                event.getId(), event.getStatus());

        String processInstanceId = event.getId();
        Map<String, Object> processVariables = event.getProcessVariables();

        // 处理审批通过
        if (BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            handleApproved(processInstanceId, processVariables);
            return;
        }

        // 处理审批拒绝
        if (BpmTaskStatusEnum.REJECT.getStatus().equals(event.getStatus())) {
            handleRejected(processInstanceId);
            return;
        }

        // 处理取消
        if (BpmTaskStatusEnum.CANCEL.getStatus().equals(event.getStatus())) {
            handleCancelled(processInstanceId);
            return;
        }

        log.info("【服务执行监听】流程状态非最终状态，跳过处理。status={}", event.getStatus());
    }

    /**
     * 处理审批通过
     */
    private void handleApproved(String processInstanceId, Map<String, Object> processVariables) {
        try {
            // 根据流程实例ID获取申请记录
            ServiceExecutionDO execution = serviceExecutionService.getByProcessInstanceId(processInstanceId);
            if (execution == null) {
                log.warn("【服务执行监听】未找到申请记录，processInstanceId={}", processInstanceId);
                return;
            }

            // 从流程变量中获取执行人ID列表（审批时选择的）
            List<Long> executorIds = parseExecutorIds(processVariables);
            if (CollUtil.isNotEmpty(executorIds)) {
                // 先设置执行人到业务记录
                serviceExecutionService.setExecutors(execution.getId(), executorIds);
                log.info("【服务执行监听】设置执行人成功。executionId={}, executorCount={}",
                        execution.getId(), executorIds.size());
            }

            // 调用 Service 处理审批通过（创建轮次并设置执行人）
            Long roundId = serviceExecutionService.handleApproved(execution.getId(), processInstanceId);
            log.info("【服务执行监听】审批通过处理完成。executionId={}, roundId={}", execution.getId(), roundId);

        } catch (Exception e) {
            log.error("【服务执行监听】处理审批通过事件失败。error={}", e.getMessage(), e);
            // 不抛出异常，避免影响流程
        }
    }

    /**
     * 从流程变量中解析执行人ID列表
     */
    private List<Long> parseExecutorIds(Map<String, Object> processVariables) {
        if (processVariables == null) {
            return new ArrayList<>();
        }

        Object executorIdsObj = processVariables.get("executorIds");
        if (executorIdsObj == null) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();

        if (executorIdsObj instanceof List) {
            for (Object item : (List<?>) executorIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (executorIdsObj instanceof Collection) {
            for (Object item : (Collection<?>) executorIdsObj) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        } else if (executorIdsObj.getClass().isArray()) {
            Object[] array = (Object[]) executorIdsObj;
            for (Object item : array) {
                Long userId = convertToLong(item);
                if (userId != null) {
                    result.add(userId);
                }
            }
        }

        return result;
    }

    /**
     * 转换为 Long 类型
     */
    private Long convertToLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            log.warn("【服务执行监听】转换用户ID失败: {}", obj);
            return null;
        }
    }

    /**
     * 处理审批拒绝
     */
    private void handleRejected(String processInstanceId) {
        try {
            ServiceExecutionDO execution = serviceExecutionService.getByProcessInstanceId(processInstanceId);
            if (execution == null) {
                return;
            }
            serviceExecutionService.updateStatus(execution.getId(), 2); // 已拒绝
            log.info("【服务执行监听】审批拒绝，已更新状态。executionId={}", execution.getId());
        } catch (Exception e) {
            log.error("【服务执行监听】处理审批拒绝事件失败。error={}", e.getMessage(), e);
        }
    }

    /**
     * 处理取消
     */
    private void handleCancelled(String processInstanceId) {
        try {
            ServiceExecutionDO execution = serviceExecutionService.getByProcessInstanceId(processInstanceId);
            if (execution == null) {
                return;
            }
            serviceExecutionService.updateStatus(execution.getId(), 3); // 已取消
            log.info("【服务执行监听】已取消，已更新状态。executionId={}", execution.getId());
        } catch (Exception e) {
            log.error("【服务执行监听】处理取消事件失败。error={}", e.getMessage(), e);
        }
    }

}
