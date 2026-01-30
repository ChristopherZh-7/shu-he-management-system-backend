package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * 项目工作记录 DO
 * 
 * 用于记录每个项目/服务项的日常工作内容
 */
@TableName("project_management_record")
@KeySequence("project_management_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectWorkRecordDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    // ========== 项目关联 ==========

    /**
     * 项目ID
     * 
     * 关联 {@link ProjectDO#getId()} 或 {@link SecurityOperationContractDO#getId()}
     */
    private Long projectId;

    /**
     * 项目类型
     * 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer projectType;

    /**
     * 项目名称（冗余便于查询）
     */
    private String projectName;

    // ========== 服务项关联（可选） ==========

    /**
     * 服务项ID（可选）
     * 
     * 关联 {@link ServiceItemDO#getId()}
     */
    private Long serviceItemId;

    /**
     * 服务项名称（冗余）
     */
    private String serviceItemName;

    // ========== 记录内容 ==========

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 工作类型
     * patrol-巡检, meeting-会议, report-报告, incident-事件处理, 
     * training-培训, maintenance-维护, other-其他
     */
    private String workType;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 附件URL（JSON数组）
     */
    private String attachments;

    /**
     * 备注
     */
    private String remark;

    // ========== 记录人信息 ==========

    /**
     * 记录人姓名（冗余）
     * 
     * 注意：creator 字段在 BaseDO 中已定义
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
