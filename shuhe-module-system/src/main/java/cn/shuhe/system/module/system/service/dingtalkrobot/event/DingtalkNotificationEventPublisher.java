package cn.shuhe.system.module.system.service.dingtalkrobot.event;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉通知事件发布器
 * 
 * 业务模块通过此类发布通知事件
 *
 * @author shuhe
 */
@Slf4j
@Component
public class DingtalkNotificationEventPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布通知事件
     *
     * @param eventType 事件类型
     * @param eventModule 事件模块
     * @param businessId 业务ID
     * @param businessNo 业务编号
     * @param variables 模板变量
     * @param ownerUserId 负责人用户ID
     * @param creatorUserId 创建人用户ID
     */
    public void publish(String eventType, String eventModule,
                       Long businessId, String businessNo,
                       Map<String, Object> variables,
                       Long ownerUserId, Long creatorUserId) {
        DingtalkNotificationEvent event = DingtalkNotificationEvent.builder(this)
                .eventType(eventType)
                .eventModule(eventModule)
                .businessId(businessId)
                .businessNo(businessNo)
                .variables(variables)
                .ownerUserId(ownerUserId)
                .creatorUserId(creatorUserId)
                .build();

        log.debug("发布钉钉通知事件：{}/{}, businessId={}", eventModule, eventType, businessId);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布通知事件（简化版本，不需要@人员）
     */
    public void publish(String eventType, String eventModule,
                       Long businessId, String businessNo,
                       Map<String, Object> variables) {
        publish(eventType, eventModule, businessId, businessNo, variables, null, null);
    }

    /**
     * 发布CRM模块事件
     */
    public void publishCrmEvent(String eventType, Long businessId, String businessNo,
                               Map<String, Object> variables, Long ownerUserId) {
        publish(eventType, "crm", businessId, businessNo, variables, ownerUserId, null);
    }

    /**
     * 发布项目模块事件
     */
    public void publishProjectEvent(String eventType, Long businessId, String businessNo,
                                   Map<String, Object> variables, Long ownerUserId) {
        publish(eventType, "project", businessId, businessNo, variables, ownerUserId, null);
    }

    /**
     * 发布系统模块事件
     */
    public void publishSystemEvent(String eventType, Long businessId, String businessNo,
                                  Map<String, Object> variables) {
        publish(eventType, "system", businessId, businessNo, variables, null, null);
    }

}
