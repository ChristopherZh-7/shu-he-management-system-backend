package cn.shuhe.system.module.system.service.dingtalk;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务发起确认回调接口
 * 
 * 由 project 模块实现，system 模块通过此接口获取服务发起相关信息
 */
public interface ServiceLaunchConfirmCallback {

    /**
     * 执行人信息
     */
    @Data
    class MemberInfo {
        private Long id;
        private Long launchId;
        private Long userId;
        private String userName;
        private Integer confirmStatus;
        private String oaProcessInstanceId;
    }

    /**
     * 服务发起信息
     */
    @Data
    class LaunchInfo {
        private Long id;
        private Long serviceItemId;
        private String serviceItemName;
        private String serviceType;       // 服务类型代码（字典值）
        private String serviceTypeName;   // 服务类型中文名称（字典标签）
        private String customerName;
        private String destination;
        private String reason;
        private LocalDateTime planStartTime;
        private LocalDateTime planEndTime;
    }

    /**
     * 获取执行人信息
     */
    MemberInfo getMemberInfo(Long memberId);

    /**
     * 获取服务发起信息
     */
    LaunchInfo getLaunchInfo(Long launchId);

    /**
     * 更新确认状态
     * 
     * @param memberId 执行人记录ID
     * @param confirmStatus 确认状态
     * @param oaProcessInstanceId 钉钉OA审批实例ID
     */
    void updateConfirmStatus(Long memberId, Integer confirmStatus, String oaProcessInstanceId);

    /**
     * 获取用户的钉钉userId
     */
    String getDingtalkUserId(Long userId);

}
