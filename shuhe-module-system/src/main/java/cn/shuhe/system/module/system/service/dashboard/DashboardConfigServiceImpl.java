package cn.shuhe.system.module.system.service.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardConfigSaveReqVO;
import cn.shuhe.system.module.system.dal.dataobject.dashboard.DashboardConfigDO;
import cn.shuhe.system.module.system.dal.mysql.dashboard.DashboardConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仪表板配置 Service 实现类
 */
@Service
public class DashboardConfigServiceImpl implements DashboardConfigService {

    @Resource
    private DashboardConfigMapper dashboardConfigMapper;

    @Override
    public DashboardConfigDO getConfig(Long userId, String pageType) {
        return dashboardConfigMapper.selectByUserIdAndPageType(userId, pageType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConfig(Long userId, DashboardConfigSaveReqVO saveReq) {
        // 查询是否已存在配置
        DashboardConfigDO existConfig = dashboardConfigMapper.selectByUserIdAndPageType(
                userId, saveReq.getPageType());
        
        if (existConfig != null) {
            // 更新已有配置
            existConfig.setLayoutConfig(saveReq.getLayoutConfig());
            dashboardConfigMapper.updateById(existConfig);
            return existConfig.getId();
        } else {
            // 创建新配置
            DashboardConfigDO config = DashboardConfigDO.builder()
                    .userId(userId)
                    .pageType(saveReq.getPageType())
                    .layoutConfig(saveReq.getLayoutConfig())
                    .build();
            dashboardConfigMapper.insert(config);
            return config.getId();
        }
    }

    @Override
    public void resetConfig(Long userId, String pageType) {
        // 使用物理删除，避免软删除导致的唯一键冲突
        dashboardConfigMapper.physicalDeleteByUserIdAndPageType(userId, pageType);
    }

    @Override
    public String getDefaultLayoutConfig(String pageType) {
        // 返回默认布局配置
        if ("analytics".equals(pageType)) {
            return getDefaultAnalyticsLayout();
        } else if ("workspace".equals(pageType)) {
            return getDefaultWorkspaceLayout();
        }
        return "[]";
    }

    /**
     * 分析页默认布局
     */
    private String getDefaultAnalyticsLayout() {
        return """
            [
                {"i":"stat-project","x":0,"y":0,"w":3,"h":2,"componentType":"stat-project"},
                {"i":"stat-contract","x":3,"y":0,"w":3,"h":2,"componentType":"stat-contract"},
                {"i":"stat-task","x":6,"y":0,"w":3,"h":2,"componentType":"stat-task"},
                {"i":"stat-profit","x":9,"y":0,"w":3,"h":2,"componentType":"stat-profit"},
                {"i":"chart-trend","x":0,"y":2,"w":8,"h":4,"componentType":"chart-trend"},
                {"i":"chart-pie-project","x":8,"y":2,"w":4,"h":4,"componentType":"chart-pie-project"},
                {"i":"chart-rank-dept","x":0,"y":6,"w":4,"h":4,"componentType":"chart-rank-dept"},
                {"i":"list-todo","x":4,"y":6,"w":4,"h":4,"componentType":"list-todo"},
                {"i":"chart-pie-task","x":8,"y":6,"w":4,"h":4,"componentType":"chart-pie-task"}
            ]
            """;
    }

    /**
     * 工作台默认布局
     */
    private String getDefaultWorkspaceLayout() {
        return """
            [
                {"i":"stat-project","x":0,"y":0,"w":3,"h":2,"componentType":"stat-project"},
                {"i":"stat-task","x":3,"y":0,"w":3,"h":2,"componentType":"stat-task"},
                {"i":"stat-contract","x":6,"y":0,"w":3,"h":2,"componentType":"stat-contract"},
                {"i":"stat-profit","x":9,"y":0,"w":3,"h":2,"componentType":"stat-profit"},
                {"i":"list-todo","x":0,"y":2,"w":6,"h":5,"componentType":"list-todo"},
                {"i":"list-recent","x":6,"y":2,"w":6,"h":5,"componentType":"list-recent"}
            ]
            """;
    }

}
