package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 项目轮次 DO
 * 
 * 一个项目可以有多个执行轮次，例如一个渗透测试合同可能包含多次测试
 */
@TableName("project_round")
@KeySequence("project_round_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoundDO extends BaseDO {

    /**
     * 轮次ID
     */
    @TableId
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 轮次序号（第几次）
     */
    private Integer roundNo;

    /**
     * 轮次名称（如：第1次渗透测试）
     */
    private String name;

    // ========== 时间信息 ==========

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

    // ========== 执行信息（支持多人）==========

    /**
     * 执行人ID列表（JSON数组）
     */
    private String executorIds;

    /**
     * 执行人姓名列表（逗号分隔）
     */
    private String executorNames;

    /**
     * 状态：0待执行 1执行中 2已完成 3已取消
     */
    private Integer status;

    /**
     * 进度 0-100
     */
    private Integer progress;

    // ========== 结果信息 ==========

    /**
     * 执行结果/报告摘要
     */
    private String result;

    /**
     * 附件（JSON数组）
     */
    private String attachments;

    /**
     * 备注
     */
    private String remark;

}
