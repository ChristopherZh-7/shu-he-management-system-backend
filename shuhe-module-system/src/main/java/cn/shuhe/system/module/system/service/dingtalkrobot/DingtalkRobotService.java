package cn.shuhe.system.module.system.service.dingtalkrobot;

import java.util.List;
import jakarta.validation.Valid;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotMessageDO;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService.RobotSendResult;

/**
 * 钉钉群机器人 Service 接口
 *
 * @author shuhe
 */
public interface DingtalkRobotService {

    // ==================== 机器人配置管理 ====================

    /**
     * 创建群机器人配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createDingtalkRobot(@Valid DingtalkRobotSaveReqVO createReqVO);

    /**
     * 更新群机器人配置
     *
     * @param updateReqVO 更新信息
     */
    void updateDingtalkRobot(@Valid DingtalkRobotSaveReqVO updateReqVO);

    /**
     * 删除群机器人配置
     *
     * @param id 编号
     */
    void deleteDingtalkRobot(Long id);

    /**
     * 批量删除群机器人配置
     *
     * @param ids 编号列表
     */
    void deleteDingtalkRobotListByIds(List<Long> ids);

    /**
     * 获得群机器人配置
     *
     * @param id 编号
     * @return 群机器人配置
     */
    DingtalkRobotDO getDingtalkRobot(Long id);

    /**
     * 获得群机器人配置分页
     *
     * @param pageReqVO 分页查询
     * @return 群机器人配置分页
     */
    PageResult<DingtalkRobotDO> getDingtalkRobotPage(DingtalkRobotPageReqVO pageReqVO);

    /**
     * 获取启用状态的群机器人配置列表
     *
     * @return 启用的群机器人配置列表
     */
    List<DingtalkRobotDO> getEnabledDingtalkRobotList();

    // ==================== 消息发送 ====================

    /**
     * 发送文本消息
     *
     * @param robotId 机器人ID
     * @param content 文本内容
     * @param atMobiles @的手机号列表
     * @param atUserIds @的用户ID列表
     * @param isAtAll 是否@所有人
     * @return 发送结果
     */
    RobotSendResult sendTextMessage(Long robotId, String content, 
                                    List<String> atMobiles, List<String> atUserIds, 
                                    boolean isAtAll);

    /**
     * 发送Markdown消息
     *
     * @param robotId 机器人ID
     * @param title 标题
     * @param text Markdown内容
     * @param atMobiles @的手机号列表
     * @param atUserIds @的用户ID列表
     * @param isAtAll 是否@所有人
     * @return 发送结果
     */
    RobotSendResult sendMarkdownMessage(Long robotId, String title, String text,
                                        List<String> atMobiles, List<String> atUserIds,
                                        boolean isAtAll);

    /**
     * 发送链接消息
     *
     * @param robotId 机器人ID
     * @param title 标题
     * @param text 描述
     * @param messageUrl 跳转链接
     * @param picUrl 图片链接
     * @return 发送结果
     */
    RobotSendResult sendLinkMessage(Long robotId, String title, String text,
                                    String messageUrl, String picUrl);

    /**
     * 发送ActionCard消息（单按钮）
     *
     * @param robotId 机器人ID
     * @param title 标题
     * @param text Markdown内容
     * @param singleTitle 按钮文字
     * @param singleURL 按钮链接
     * @return 发送结果
     */
    RobotSendResult sendActionCardMessage(Long robotId, String title, String text,
                                          String singleTitle, String singleURL);

    /**
     * 测试机器人发送
     *
     * @param robotId 机器人ID
     * @return 发送结果
     */
    RobotSendResult testRobotSend(Long robotId);

    // ==================== 消息记录 ====================

    /**
     * 获得消息记录分页
     *
     * @param pageReqVO 分页查询
     * @return 消息记录分页
     */
    PageResult<DingtalkRobotMessageDO> getMessagePage(DingtalkRobotMessagePageReqVO pageReqVO);

}
