package cn.shuhe.system.module.project.service.listener;

import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务执行申请审批结果的监听器
 * 
 * 当审批通过后，自动创建执行轮次
 */
@Component
@Slf4j
public class ServiceExecutionStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 服务执行申请的流程定义 Key
     */
    public static final String PROCESS_DEFINITION_KEY = "service-execution-apply";

    @Resource
    private ProjectRoundService projectRoundService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("【服务执行监听】收到流程状态变更事件，processInstanceId={}, businessKey={}, status={}",
                event.getId(), event.getBusinessKey(), event.getStatus());

        // 只处理审批通过的情况
        if (!BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            log.info("【服务执行监听】流程状态非审批通过，跳过处理。status={}", event.getStatus());
            return;
        }

        try {
            // businessKey 格式：serviceItemId
            Long serviceItemId = Long.parseLong(event.getBusinessKey());
            String processInstanceId = event.getId();

            // 创建执行轮次
            Long roundId = projectRoundService.createRoundByServiceItem(serviceItemId, processInstanceId);
            log.info("【服务执行监听】审批通过，已创建执行轮次。serviceItemId={}, roundId={}", serviceItemId, roundId);

        } catch (Exception e) {
            log.error("【服务执行监听】处理审批通过事件失败。businessKey={}, error={}",
                    event.getBusinessKey(), e.getMessage(), e);
            // 不抛出异常，避免影响流程
        }
    }

}
