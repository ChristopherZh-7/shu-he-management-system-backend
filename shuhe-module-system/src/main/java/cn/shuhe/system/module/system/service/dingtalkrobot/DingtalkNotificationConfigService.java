package cn.shuhe.system.module.system.service.dingtalkrobot;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationLogDO;
import cn.shuhe.system.framework.common.pojo.PageResult;

/**
 * 钉钉通知场景配置 Service 接口
 *
 * @author shuhe
 */
public interface DingtalkNotificationConfigService {

    // ==================== 配置管理 ====================

    /**
     * 创建通知场景配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createNotificationConfig(@Valid DingtalkNotificationConfigSaveReqVO createReqVO);

    /**
     * 更新通知场景配置
     *
     * @param updateReqVO 更新信息
     */
    void updateNotificationConfig(@Valid DingtalkNotificationConfigSaveReqVO updateReqVO);

    /**
     * 删除通知场景配置
     *
     * @param id 编号
     */
    void deleteNotificationConfig(Long id);

    /**
     * 获得通知场景配置
     *
     * @param id 编号
     * @return 通知场景配置
     */
    DingtalkNotificationConfigDO getNotificationConfig(Long id);

    /**
     * 获得通知场景配置分页
     *
     * @param pageReqVO 分页查询
     * @return 通知场景配置分页
     */
    PageResult<DingtalkNotificationConfigDO> getNotificationConfigPage(DingtalkNotificationConfigPageReqVO pageReqVO);

    /**
     * 根据事件类型和模块获取启用的配置列表
     *
     * @param eventType 事件类型
     * @param eventModule 事件模块
     * @return 配置列表
     */
    List<DingtalkNotificationConfigDO> getEnabledConfigsByEvent(String eventType, String eventModule);

    // ==================== 通知发送 ====================

    /**
     * 触发事件通知
     * 根据事件类型查找配置并发送通知
     *
     * @param eventType 事件类型
     * @param eventModule 事件模块
     * @param businessId 业务ID
     * @param businessNo 业务编号
     * @param variables 模板变量（用于替换模板中的${xxx}）
     * @param ownerUserId 负责人用户ID（用于@负责人）
     * @param creatorUserId 创建人用户ID（用于@创建人）
     */
    void triggerNotification(String eventType, String eventModule, 
                            Long businessId, String businessNo,
                            Map<String, Object> variables,
                            Long ownerUserId, Long creatorUserId);

    // ==================== 日志查询 ====================

    /**
     * 获得发送日志分页
     *
     * @param pageReqVO 分页查询
     * @return 发送日志分页
     */
    PageResult<DingtalkNotificationLogDO> getNotificationLogPage(DingtalkNotificationLogPageReqVO pageReqVO);

    /**
     * 获取支持的事件类型列表
     *
     * @return 事件类型列表
     */
    List<Map<String, Object>> getSupportedEventTypes();

    /**
     * 获取支持的事件模块列表
     *
     * @return 事件模块列表
     */
    List<Map<String, String>> getSupportedEventModules();

}
