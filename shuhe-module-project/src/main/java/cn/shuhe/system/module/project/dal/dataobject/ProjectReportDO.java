package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 项目周报/汇报 DO
 *
 * 管理层填写的项目进展汇报，包含项目进展、遇到的问题、需要的资源、风险提示等
 */
@TableName("project_report")
@KeySequence("project_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReportDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目类型 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer projectType;

    /**
     * 项目名称（冗余）
     */
    private String projectName;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 周数（1-53）
     */
    private Integer weekNumber;

    /**
     * 本周开始日期（周一）
     */
    private LocalDate weekStartDate;

    /**
     * 本周结束日期（周五）
     */
    private LocalDate weekEndDate;

    /**
     * 项目进展
     */
    private String progress;

    /**
     * 遇到的问题
     */
    private String issues;

    /**
     * 需要的资源/协调事项
     */
    private String resources;

    /**
     * 风险提示
     */
    private String risks;

    /**
     * 下周计划
     */
    private String nextWeekPlan;

    /**
     * 附件URL（JSON数组）
     */
    private String attachments;

    /**
     * 备注
     */
    private String remark;

    /**
     * 记录人姓名（冗余）
     */
    private String creatorName;

    /**
     * 记录人部门ID
     */
    private Long deptId;

    /**
     * 部门名称（冗余）
     */
    private String deptName;

}
