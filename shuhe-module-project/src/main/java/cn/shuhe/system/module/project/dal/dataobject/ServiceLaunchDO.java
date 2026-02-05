package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 统一服务发起 DO
 * 
 * 整合外出请求和服务执行，统一服务发起流程
 * 支持：普通服务执行、外出服务、跨部门服务
 */
@TableName("project_service_launch")
@KeySequence("project_service_launch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLaunchDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 关联合同ID
     */
    private Long contractId;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联服务项ID
     */
    private Long serviceItemId;

    /**
     * 服务项归属部门ID
     */
    private Long serviceItemDeptId;

    /**
     * 执行部门ID
     */
    private Long executeDeptId;

    /**
     * 发起人ID
     */
    private Long requestUserId;

    /**
     * 发起人部门ID
     */
    private Long requestDeptId;

    /**
     * 是否外出：0否 1是
     */
    private Boolean isOutside;

    /**
     * 是否跨部门：0否 1是
     * 判断条件：服务项归属部门 != 执行部门
     */
    private Boolean isCrossDept;

    /**
     * 是否排队：0否 1是
     * 当目标部门无空闲员工时，可选择排队等待
     */
    private Boolean isQueued;

    /**
     * 期望执行人ID（申请时可选择）
     */
    private Long expectedExecutorId;

    /**
     * 期望执行人姓名
     */
    private String expectedExecutorName;

    /**
     * 期望开始时间（排队时填写）
     */
    private LocalDateTime expectedStartTime;

    /**
     * 排队顺序（排队中时有效）
     */
    private Integer queueOrder;

    /**
     * 是否代发起：0否 1是
     * 代发起场景：当前用户帮其他部门发起该部门的服务项
     */
    private Boolean isDelegation;

    /**
     * 被代发起人ID（代发起时必填）
     * 实际业务归属的用户
     */
    private Long delegateUserId;

    /**
     * 外出地点（仅外出时有值）
     */
    private String destination;

    /**
     * 外出事由（仅外出时有值）
     */
    private String reason;

    /**
     * 计划开始时间
     */
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime planEndTime;

    /**
     * 执行人ID列表（JSON数组，审批时选择）
     */
    private String executorIds;

    /**
     * 执行人姓名列表（逗号分隔）
     */
    private String executorNames;

    /**
     * 状态：0待审批 1已通过 2已拒绝 3已取消
     */
    private Integer status;

    /**
     * 工作流流程实例ID
     */
    private String processInstanceId;

    /**
     * 创建的轮次ID（审批通过后）
     */
    private Long roundId;

    /**
     * 审批人所在部门ID
     * 如果用户选择的部门没有负责人，会向上递归查找
     */
    private Long approverDeptId;

    /**
     * 是否需要在审批时选择执行的子部门
     * 当审批人是父部门负责人时，需要先选择哪个子部门执行
     */
    private Boolean needSelectExecuteDept;

    /**
     * 实际执行部门ID（审批时选择的子部门）
     * 如果不需要选择子部门，则与executeDeptId相同
     */
    private Long actualExecuteDeptId;

    /**
     * 备注
     */
    private String remark;

    // ========== 渗透测试附件 ==========

    /**
     * 授权书附件URL（JSON数组）
     */
    private String authorizationUrls;

    /**
     * 测试范围附件URL（JSON数组）
     */
    private String testScopeUrls;

    /**
     * 账号密码附件URL（JSON数组）
     */
    private String credentialsUrls;

}
