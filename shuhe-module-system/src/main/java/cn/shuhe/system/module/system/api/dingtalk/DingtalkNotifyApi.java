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
