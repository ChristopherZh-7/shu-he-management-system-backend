package cn.shuhe.system.module.crm.service.business.listener;

import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.business.CrmBusinessServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 商机审批结果监听器
 * 监听 BPM 流程 {@code crm-business-audit} 的事件：
 * - 审批全部通过 → 创建钉钉群（纯通知）
 * - 审批驳回 → 工作通知商机负责人
 */
@Component
public class CrmBusinessStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private CrmBusinessService businessService;

    @Override
    public String getProcessDefinitionKey() {
        return CrmBusinessServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        businessService.updateBusinessAuditStatus(
                Long.parseLong(event.getBusinessKey()),
                event.getStatus());
    }

}
