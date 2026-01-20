package cn.shuhe.system.module.project.service.listener;

import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 渗透测试发起流程审批结果的监听器
 * 
 * 当审批通过后，自动创建执行轮次
 */
@Component
@Slf4j
public class PenetrationTestStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 渗透测试发起的流程定义 Key
     */
    public static final String PROCESS_DEFINITION_KEY = "penetration_test_start";

    @Resource
    private ProjectRoundService projectRoundService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        log.info("【渗透测试监听】收到流程状态变更事件，processInstanceId={}, status={}",
                event.getId(), event.getStatus());

        // 只处理审批通过的情况
        if (!BpmTaskStatusEnum.APPROVE.getStatus().equals(event.getStatus())) {
            log.info("【渗透测试监听】流程状态非审批通过，跳过处理。status={}", event.getStatus());
            return;
        }

        try {
            // 从流程变量中获取 serviceItemId
            Map<String, Object> processVariables = event.getProcessVariables();
            if (processVariables == null) {
                log.warn("【渗透测试监听】流程变量为空，无法获取 serviceItemId");
                return;
            }

            Object serviceItemIdObj = processVariables.get("serviceItemId");
            if (serviceItemIdObj == null) {
                log.warn("【渗透测试监听】流程变量中未找到 serviceItemId");
                return;
            }

            Long serviceItemId;
            if (serviceItemIdObj instanceof Number) {
                serviceItemId = ((Number) serviceItemIdObj).longValue();
            } else {
                serviceItemId = Long.parseLong(serviceItemIdObj.toString());
            }

            String processInstanceId = event.getId();

            // 从流程变量中获取计划时间
            LocalDateTime planStartTime = parseDateTime(processVariables.get("planStartTime"));
            LocalDateTime planEndTime = parseDateTime(processVariables.get("planEndTime"));

            log.info("【渗透测试监听】获取到计划时间：planStartTime={}, planEndTime={}", planStartTime, planEndTime);

            // 创建执行轮次
            Long roundId = projectRoundService.createRoundByServiceItem(serviceItemId, processInstanceId, planStartTime, planEndTime);
            log.info("【渗透测试监听】审批通过，已创建执行轮次。serviceItemId={}, roundId={}, processInstanceId={}",
                    serviceItemId, roundId, processInstanceId);

        } catch (Exception e) {
            log.error("【渗透测试监听】处理审批通过事件失败。processInstanceId={}, error={}",
                    event.getId(), e.getMessage(), e);
            // 不抛出异常，避免影响流程
        }
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        try {
            return LocalDateTime.parse(value.toString(), DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("【渗透测试监听】解析日期时间失败：value={}, error={}", value, e.getMessage());
            return null;
        }
    }

}
