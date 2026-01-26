package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 服务执行申请 DO
 * 
 * 用于申请执行服务项（除外出服务外）
 * 审批通过后自动创建执行轮次
 */
@TableName("project_service_execution")
@KeySequence("project_service_execution_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceExecutionDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联服务项ID
     */
    private Long serviceItemId;

    /**
     * 发起人ID
     */
    private Long requestUserId;

    /**
     * 发起人部门ID
     */
    private Long requestDeptId;

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
