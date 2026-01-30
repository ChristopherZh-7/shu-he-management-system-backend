package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 日常管理记录 DO（按周记录）
 * 
 * 用于记录员工每周的日常管理工作，包括周一到周五的工作内容、周总结和下周计划
 */
@TableName("daily_management_record")
@KeySequence("daily_management_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class DailyManagementRecordDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

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
     * 周一工作内容
     */
    private String mondayContent;

    /**
     * 周二工作内容
     */
    private String tuesdayContent;

    /**
     * 周三工作内容
     */
    private String wednesdayContent;

    /**
     * 周四工作内容
     */
    private String thursdayContent;

    /**
     * 周五工作内容
     */
    private String fridayContent;

    /**
     * 本周总结
     */
    private String weeklySummary;

    /**
     * 下周计划
     */
    private String nextWeekPlan;

    /**
     * 附件URL列表（JSON数组）
     */
    private String attachments;

    /**
     * 记录人姓名（冗余字段，方便查询展示）
     */
    private String creatorName;

    /**
     * 记录人部门ID
     */
    private Long deptId;

    /**
     * 部门名称（冗余字段）
     */
    private String deptName;

}
