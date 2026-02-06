package cn.shuhe.system.module.system.service.dingtalkrobot.event;

import lombok.*;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 钉钉通知事件
 * 
 * 业务模块在完成特定操作后发布此事件，触发钉钉群通知
 *
 * @author shuhe
 */
@Getter
public class DingtalkNotificationEvent extends ApplicationEvent {

    /**
     * 事件类型
     * 如：contract_create, project_create 等
     */
    private final String eventType;

    /**
     * 事件模块
     * 如：crm, project, bpm 等
     */
    private final String eventModule;

    /**
     * 业务数据ID
     */
    private final Long businessId;

    /**
     * 业务编号（如合同编号）
     */
    private final String businessNo;

    /**
     * 模板变量
     * 用于替换消息模板中的 ${xxx} 占位符
     */
    private final Map<String, Object> variables;

    /**
     * 负责人用户ID（用于@负责人）
     */
    private final Long ownerUserId;

    /**
     * 创建人用户ID（用于@创建人）
     */
    private final Long creatorUserId;

    public DingtalkNotificationEvent(Object source, String eventType, String eventModule,
                                     Long businessId, String businessNo,
                                     Map<String, Object> variables,
                                     Long ownerUserId, Long creatorUserId) {
        super(source);
        this.eventType = eventType;
        this.eventModule = eventModule;
        this.businessId = businessId;
        this.businessNo = businessNo;
        this.variables = variables;
        this.ownerUserId = ownerUserId;
        this.creatorUserId = creatorUserId;
    }

    /**
     * 创建事件的Builder
     */
    public static Builder builder(Object source) {
        return new Builder(source);
    }

    /**
     * Builder 类，方便构建事件
     */
    public static class Builder {
        private final Object source;
        private String eventType;
        private String eventModule;
        private Long businessId;
        private String businessNo;
        private Map<String, Object> variables;
        private Long ownerUserId;
        private Long creatorUserId;

        public Builder(Object source) {
            this.source = source;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder eventModule(String eventModule) {
            this.eventModule = eventModule;
            return this;
        }

        public Builder businessId(Long businessId) {
            this.businessId = businessId;
            return this;
        }

        public Builder businessNo(String businessNo) {
            this.businessNo = businessNo;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public Builder ownerUserId(Long ownerUserId) {
            this.ownerUserId = ownerUserId;
            return this;
        }

        public Builder creatorUserId(Long creatorUserId) {
            this.creatorUserId = creatorUserId;
            return this;
        }

        public DingtalkNotificationEvent build() {
            return new DingtalkNotificationEvent(source, eventType, eventModule,
                    businessId, businessNo, variables, ownerUserId, creatorUserId);
        }
    }

}
