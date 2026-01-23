package cn.shuhe.system.module.system.service.dingtalk;

import java.time.LocalDateTime;

/**
 * 外出确认回调接口
 * 
 * 用于跨模块调用，由 project 模块实现
 */
public interface OutsideConfirmCallback {

    /**
     * 外出人员信息
     */
    @lombok.Data
    class OutsideMemberInfo {
        /** 外出人员记录ID */
        private Long id;
        /** 外出请求ID */
        private Long requestId;
        /** 用户ID */
        private Long userId;
        /** 用户姓名 */
        private String userName;
        /** 确认状态 */
        private Integer confirmStatus;
        /** OA审批实例ID */
        private String oaProcessInstanceId;
    }

    /**
     * 外出请求信息
     */
    @lombok.Data
    class OutsideRequestInfo {
        /** 外出请求ID */
        private Long id;
        /** 项目ID */
        private Long projectId;
        /** 项目名称 */
        private String projectName;
        /** 外出地点 */
        private String destination;
        /** 外出事由 */
        private String reason;
        /** 计划开始时间 */
        private LocalDateTime planStartTime;
        /** 计划结束时间 */
        private LocalDateTime planEndTime;
    }

    /**
     * 获取外出人员信息
     * 
     * @param memberId 外出人员记录ID
     * @return 外出人员信息
     */
    OutsideMemberInfo getOutsideMemberInfo(Long memberId);

    /**
     * 获取外出请求信息
     * 
     * @param requestId 外出请求ID
     * @return 外出请求信息
     */
    OutsideRequestInfo getOutsideRequestInfo(Long requestId);

    /**
     * 更新确认状态
     * 
     * @param memberId 外出人员记录ID
     * @param confirmStatus 确认状态：0未确认 1已确认 2已提交OA
     * @param oaProcessInstanceId OA审批实例ID（可为null）
     */
    void updateConfirmStatus(Long memberId, Integer confirmStatus, String oaProcessInstanceId);

}
