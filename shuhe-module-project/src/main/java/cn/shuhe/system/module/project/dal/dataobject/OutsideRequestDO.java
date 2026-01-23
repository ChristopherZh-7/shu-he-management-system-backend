package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 外出请求 DO
 * 
 * 用于跨部门人员借调/外出协助
 * 发起人向目标部门申请人员外出协助
 */
@TableName("project_outside_request")
@KeySequence("project_outside_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutsideRequestDO extends BaseDO {

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
     * 关联服务项ID（可选）
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
     * 目标部门ID（向哪个部门申请人员）
     */
    private Long targetDeptId;

    /**
     * 外出地点/客户现场
     */
    private String destination;

    /**
     * 外出事由
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
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 状态：0待审批 1已通过 2已拒绝 3已完成 4已取消
     */
    private Integer status;

    /**
     * 工作流流程实例ID
     */
    private String processInstanceId;

    /**
     * 备注
     */
    private String remark;

}
