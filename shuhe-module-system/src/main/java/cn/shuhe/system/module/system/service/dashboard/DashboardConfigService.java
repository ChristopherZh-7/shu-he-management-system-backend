package cn.shuhe.system.module.system.service.dashboard;

import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardConfigSaveReqVO;
import cn.shuhe.system.module.system.dal.dataobject.dashboard.DashboardConfigDO;

/**
 * 仪表板配置 Service 接口
 */
public interface DashboardConfigService {

    /**
     * 获取用户的仪表板配置
     *
     * @param userId   用户ID
     * @param pageType 页面类型
     * @return 仪表板配置
     */
    DashboardConfigDO getConfig(Long userId, String pageType);

    /**
     * 保存用户的仪表板配置
     *
     * @param userId  用户ID
     * @param saveReq 保存请求
     * @return 配置ID
     */
    Long saveConfig(Long userId, DashboardConfigSaveReqVO saveReq);

    /**
     * 重置用户的仪表板配置（删除自定义配置，恢复默认）
     *
     * @param userId   用户ID
     * @param pageType 页面类型
     */
    void resetConfig(Long userId, String pageType);

    /**
     * 获取默认布局配置
     *
     * @param pageType 页面类型
     * @return 默认布局配置JSON
     */
    String getDefaultLayoutConfig(String pageType);

}
