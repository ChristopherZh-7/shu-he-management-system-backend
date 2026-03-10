package cn.shuhe.system.module.system.job;

import cn.shuhe.system.framework.quartz.core.handler.JobHandler;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 钉钉用户同步计划任务
 * 每周一执行一次，同步所有启用的钉钉配置的用户数据
 *
 * @author ShuHe
 */
@Slf4j
@Component
public class DingtalkSyncJob implements JobHandler {

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Override
    public String execute(String param) {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (configs.isEmpty()) {
            log.info("[DingtalkSyncJob] 没有启用的钉钉配置，跳过同步");
            return "没有启用的钉钉配置，跳过同步";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder result = new StringBuilder();

        for (DingtalkConfigDO config : configs) {
            try {
                log.info("[DingtalkSyncJob] 开始同步钉钉用户，配置: {} (id={})", config.getName(), config.getId());
                dingtalkConfigService.syncDingtalkUser(config.getId());
                successCount++;
                result.append(String.format("配置[%s]同步成功; ", config.getName()));
            } catch (Exception e) {
                failCount++;
                log.error("[DingtalkSyncJob] 钉钉用户同步失败，配置: {} (id={})", config.getName(), config.getId(), e);
                result.append(String.format("配置[%s]同步失败: %s; ", config.getName(), e.getMessage()));
            }
        }

        String summary = String.format("完成: 成功%d个, 失败%d个. %s", successCount, failCount, result);
        log.info("[DingtalkSyncJob] {}", summary);
        return summary;
    }
}
