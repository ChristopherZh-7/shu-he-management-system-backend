package cn.shuhe.system.module.crm.service.business.listener;

import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.shuhe.system.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.business.CrmBusinessServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 商机提前投入审批结果监听器
 * 监听 BPM 流程 {@code crm-business-early-investment} 的事件：
 * - 审批全部通过 → 自动创建项目
 * - 审批驳回 → 工作通知商机负责人
 */
@Component
public class CrmBusinessEarlyInvestmentStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private CrmBusinessService businessService;

    @Override
    public String getProcessDefinitionKey() {
        return CrmBusinessServiceImpl.EARLY_INVESTMENT_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        businessService.updateEarlyInvestmentAuditStatus(
                Long.parseLong(event.getBusinessKey()),
                event.getStatus());
    }

}
