package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
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

    // ========== 项目关联（内部工作可为空） ==========

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
     * 服务类型（字典值，冗余）
     */
    private String serviceType;

    /**
     * 来源类型：internal-内部工作 manual-手工项目 service_item-服务事项
     * round-项目轮次 ticket-服务工单
     */
    private String sourceType;

    /**
     * 来源业务 ID
     */
    private Long sourceId;

    /**
     * 来源名称快照
     */
    private String sourceName;

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
     * 个人实际投入分钟数
     */
    private Integer actualMinutes;

    /**
     * 本条工作完成比例 0-100
     */
    private Integer completionPercent;

    /**
     * 工作结果/产出说明
     */
    private String workResult;

    /**
     * 产出数量
     */
    private BigDecimal outputQuantity;

    /**
     * 产出单位
     */
    private String outputUnit;

    /**
     * 核验状态：0-自报 1-已关联 2-已验收
     */
    private Integer verificationStatus;

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

    public static final String SOURCE_INTERNAL = "internal";
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_SERVICE_ITEM = "service_item";
    public static final String SOURCE_ROUND = "round";
    public static final String SOURCE_TICKET = "ticket";
    public static final int VERIFICATION_SELF_REPORTED = 0;
    public static final int VERIFICATION_LINKED = 1;
    public static final int VERIFICATION_ACCEPTED = 2;

}
