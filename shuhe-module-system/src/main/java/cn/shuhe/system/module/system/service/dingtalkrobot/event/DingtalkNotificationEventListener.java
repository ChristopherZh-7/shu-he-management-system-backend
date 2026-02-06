package cn.shuhe.system.module.system.service.dingtalkrobot.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import cn.shuhe.system.module.system.service.dingtalkrobot.DingtalkNotificationConfigService;

/**
 * 钉钉通知事件监听器
 * 
 * 监听业务事件，触发钉钉群通知发送
 *
 * @author shuhe
 */
@Slf4j
@Component
public class DingtalkNotificationEventListener {

    @Resource
    private DingtalkNotificationConfigService notificationConfigService;

    /**
     * 监听钉钉通知事件
     * 
     * 使用@Async异步处理，避免阻塞业务主流程
     */
    @Async
    @EventListener
    public void onDingtalkNotificationEvent(DingtalkNotificationEvent event) {
        log.debug("收到钉钉通知事件：{}/{}, businessId={}", 
                event.getEventModule(), event.getEventType(), event.getBusinessId());
        
        try {
            notificationConfigService.triggerNotification(
                    event.getEventType(),
                    event.getEventModule(),
                    event.getBusinessId(),
                    event.getBusinessNo(),
                    event.getVariables(),
                    event.getOwnerUserId(),
                    event.getCreatorUserId()
            );
        } catch (Exception e) {
            log.error("处理钉钉通知事件失败：{}/{}, businessId={}", 
                    event.getEventModule(), event.getEventType(), event.getBusinessId(), e);
            // 通知失败不影响业务流程，只记录日志
        }
    }

}
