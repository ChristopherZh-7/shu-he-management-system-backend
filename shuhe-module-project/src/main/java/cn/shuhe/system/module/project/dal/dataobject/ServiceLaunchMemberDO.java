package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 服务发起执行人 DO
 * 
 * 记录服务发起的执行人信息
 * 主要用于外出服务的确认和完成流程
 */
@TableName("project_service_launch_member")
@KeySequence("project_service_launch_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLaunchMemberDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 服务发起ID
     */
    private Long launchId;

    /**
     * 执行人ID
     */
    private Long userId;

    /**
     * 执行人姓名（快照）
     */
    private String userName;

    /**
     * 执行人部门ID（快照）
     */
    private Long userDeptId;

    /**
     * 执行人部门名称（快照）
     */
    private String userDeptName;

    /**
     * 确认状态：0未确认 1已确认 2已提交OA 3OA已通过
     */
    private Integer confirmStatus;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 钉钉OA审批实例ID
     */
    private String oaProcessInstanceId;

    /**
     * 完成状态：0未完成 1已完成（无附件） 2已完成（有附件）
     */
    private Integer finishStatus;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 附件URL（多个用逗号分隔）
     */
    private String attachmentUrl;

    /**
     * 完成备注
     */
    private String finishRemark;

}
