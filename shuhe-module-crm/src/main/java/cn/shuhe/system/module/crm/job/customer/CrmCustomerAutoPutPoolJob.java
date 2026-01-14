package cn.shuhe.system.module.crm.job.customer;

import cn.shuhe.system.framework.quartz.core.handler.JobHandler;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 客户自动掉入公海 Job
 *
 * @author 芋道源码
 */
@Component
public class CrmCustomerAutoPutPoolJob implements JobHandler {

    @Resource
    private CrmCustomerService customerService;

    @Override

    public String execute(String param) {
        int count = customerService.autoPutCustomerPool();
        return String.format("掉入公海客户 %s 个", count);
    }

}