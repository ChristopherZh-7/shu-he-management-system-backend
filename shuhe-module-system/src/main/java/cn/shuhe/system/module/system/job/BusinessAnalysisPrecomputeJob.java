package cn.shuhe.system.module.system.job;

import cn.shuhe.system.framework.quartz.core.handler.JobHandler;
import cn.shuhe.system.module.system.service.cost.BusinessAnalysisCacheService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 经营分析数据预计算定时任务
 * 
 * 建议配置：
 * - 任务名称：businessAnalysisPrecomputeJob
 * - CRON 表达式：0 0/30 * * * ?（每30分钟执行一次）
 * - 或者：0 0 7,12,18 * * ?（每天7点、12点、18点执行）
 * 
 * 功能说明：
 * 1. 清除现有经营分析缓存
 * 2. 预计算当前年度的经营分析数据并缓存
 * 3. 后续用户查询时直接从缓存读取，大幅提升响应速度
 *
 * @author system
 */
@Slf4j
@Component
public class BusinessAnalysisPrecomputeJob implements JobHandler {

    @Resource
    private BusinessAnalysisCacheService businessAnalysisCacheService;

    @Override
    public String execute(String param) {
        log.info("[定时任务] 开始执行经营分析数据预计算...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行预计算
            businessAnalysisCacheService.precomputeCurrentYearData();
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[定时任务] 经营分析数据预计算完成，耗时={}ms", elapsed);
            
            return String.format("预计算完成，耗时=%dms", elapsed);
        } catch (Exception e) {
            log.error("[定时任务] 经营分析数据预计算失败", e);
            return "预计算失败: " + e.getMessage();
        }
    }

}
