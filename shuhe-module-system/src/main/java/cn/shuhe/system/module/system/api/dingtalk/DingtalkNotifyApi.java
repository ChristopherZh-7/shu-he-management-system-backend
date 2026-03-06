package cn.shuhe.system.module.system.api.dingtalk;

import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 钉钉通知 API 接口
 */
public interface DingtalkNotifyApi {

    /**
     * 发送钉钉工作通知
     *
     * @param reqDTO 发送请求
     * @return 是否成功
     */
    boolean sendWorkNotice(@Valid DingtalkNotifySendReqDTO reqDTO);

    /**
     * 发送钉钉工作通知给指定部门类型的负责人
     *
     * @param deptType 部门类型 (1-安全服务, 2-安全运营, 3-数据安全)
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @return 是否成功
     */
    boolean sendWorkNoticeToDeptTypeLeaders(Integer deptType, String title, String content);

    /**
     * 根据系统用户ID获取钉钉用户ID
     *
     * @param userId 系统用户ID
     * @return 钉钉用户ID，如果找不到返回null
     */
    String getDingtalkUserIdByLocalUserId(Long userId);

    /**
     * 根据部门类型获取部门负责人用户ID列表
     *
     * @param deptType 部门类型
     * @return 负责人用户ID列表
     */
    List<Long> getLeaderUserIdsByDeptType(Integer deptType);

    /**
     * 发送互动卡片消息（带按钮）
     *
     * @param userIds 接收人系统用户ID列表
     * @param title 消息标题
     * @param content 消息内容（markdown格式）
     * @param buttonTitle 按钮文字
     * @param buttonUrl 按钮跳转URL
     * @return 是否成功
     */
    boolean sendActionCardMessage(List<Long> userIds, String title, String content, 
                                   String buttonTitle, String buttonUrl);

    /**
     * 发起钉钉OA外出申请（独立组件模式）
     *
     * @param userId 发起人系统用户ID
     * @param processCode 外出申请流程的process_code
     * @param outsideType 外出类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param duration 时长（字符串）
     * @param projectName 关联项目
     * @param reason 外出事由
     * @param destination 外出地点
     * @return OA审批实例ID，失败返回null
     */
    String startOutsideOaApproval(Long userId, String processCode,
                                   String outsideType, String startTime, String endTime,
                                   String duration, String projectName,
                                   String reason, String destination);

    /**
     * 通过单聊机器人发送私聊消息（Markdown格式）
     *
     * @param reqDTO 发送请求（包含 userIds、title、content）
     * @return 是否成功
     */
    boolean sendPrivateMessage(DingtalkNotifySendReqDTO reqDTO);

    /**
     * 创建钉钉群聊
     *
     * @param chatName    群名称
     * @param ownerUserId 群主的系统用户ID
     * @param memberUserIds 群成员的系统用户ID列表
     * @return 钉钉群会话ID（chatId），失败返回null
     */
    String createGroupChat(String chatName, Long ownerUserId, List<Long> memberUserIds);

    /**
     * 创建商机审批钉钉群
     * 若配置了场景群模板ID，使用场景群建群（机器人自动进群）；否则使用普通群。
     *
     * @param chatName      群名称
     * @param ownerUserId   群主系统用户ID
     * @param memberUserIds 群成员系统用户ID列表
     * @return 钉钉群 chatId，失败返回 null
     */
    String createBusinessAuditGroupChat(String chatName, Long ownerUserId, List<Long> memberUserIds);

    /**
     * 获取钉钉回调基础 URL（用于生成互动卡片按钮跳转链接等）
     *
     * @return 回调基础 URL，如 https://xxx.com，未配置时返回 null
     */
    String getCallbackBaseUrl();

    /**
     * 获取审批链接 baseUrl（前端/网关入口，如 http://localhost:5666）
     * 不填则用 callbackBaseUrl。用于生成「通过审批」「驳回」「修改金额」等链接
     *
     * @return 审批链接 baseUrl，未配置时返回 null
     */
    String getApproveBaseUrl();

    /**
     * 获取商机审批固定群 chatId
     * 配置后不再每个商机建群，所有审批通知发到此群。
     *
     * @return 固定群 chatId，未配置时返回 null
     */
    String getBusinessAuditChatId();

    /**
     * 获取商机审批场景群模板ID
     * 配置后使用场景群建群，机器人自动进群。
     *
     * @return 模板ID，未配置时返回 null
     */
    String getBusinessAuditTemplateId();

    /**
     * 向钉钉群发送消息
     * <p>
     * 应用的机器人需已加入该群。若发送失败（如机器人未入群），可改用工作通知。
     *
     * @param chatId  钉钉群会话ID
     * @param title   消息标题
     * @param content 消息内容（支持 markdown）
     * @return 是否发送成功
     */
    boolean sendMessageToChat(String chatId, String title, String content);

    /**
     * 将系统用户批量加入钉钉群
     * <p>
     * 用于在流程审批通过后，自动将相关人员（如提前投入人员）拉入商机群。
     *
     * @param chatId        钉钉群会话ID
     * @param memberUserIds 要加入群的系统用户ID列表
     * @return 是否成功
     */
    boolean addMembersToGroupChat(String chatId, List<Long> memberUserIds);

    /**
     * 向钉钉群发送互动卡片消息（带可点击按钮）
     * <p>
     * 应用机器人需已加入该群。支持「通过审批」等按钮，点击后跳转回调 URL。
     *
     * @param chatId      钉钉群会话ID
     * @param title       卡片标题
     * @param content     卡片内容（markdown 格式）
     * @param buttonTitle 按钮文字
     * @param buttonUrl   按钮跳转 URL
     * @return 是否发送成功
     */
    boolean sendActionCardToChat(String chatId, String title, String content,
                                 String buttonTitle, String buttonUrl);

    /**
     * 发起钉钉OA外出申请（DDBizSuite套件模式）
     * 
     * 适用于使用钉钉内置"外出"套件（DDBizSuite, biz_type: attendance.goout）的表单
     *
     * @param userId 发起人系统用户ID
     * @param processCode 外出申请流程的process_code
     * @param outsideType 外出类型（"1天内短期外出" 或 "超过1天连续外出"）
     * @param startTime 开始时间（格式根据外出类型：短期用 yyyy-MM-dd HH:mm，长期用 yyyy-MM-dd）
     * @param endTime 结束时间（格式同上）
     * @param durationValue 时长数值（短期为小时数，长期为天数，支持小数如4.12）
     * @param projectName 关联项目
     * @param reason 外出事由
     * @param destination 外出地点
     * @return OA审批实例ID，失败返回null
     */
    String startOutsideSuiteOaApproval(Long userId, String processCode,
                                        String outsideType, String startTime, String endTime,
                                        double durationValue, String projectName,
                                        String reason, String destination);

}
