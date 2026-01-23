package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 外出人员 DO
 * 
 * 记录外出请求中的人员信息
 * 由目标部门负责人在审批时选择派谁去
 */
@TableName("project_outside_member")
@KeySequence("project_outside_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutsideMemberDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外出请求ID
     */
    private Long requestId;

    /**
     * 外出人员ID
     */
    private Long userId;

    /**
     * 人员姓名（快照）
     */
    private String userName;

    /**
     * 人员部门ID（快照）
     */
    private Long userDeptId;

    /**
     * 部门名称（快照）
     */
    private String userDeptName;

    /**
     * 确认状态：0未确认 1已确认 2已提交OA 3OA已通过
     */
    private Integer confirmStatus;

    /**
     * 钉钉OA审批实例ID
     */
    private String oaProcessInstanceId;

    /**
     * 确认时间
     */
    private java.time.LocalDateTime confirmTime;

}
