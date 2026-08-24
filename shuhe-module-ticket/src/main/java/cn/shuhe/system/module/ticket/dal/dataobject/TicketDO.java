package cn.shuhe.system.module.ticket.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单 DO（{@code shuhe_ticket}）
 *
 * <p>作为跨业务的统一入口和聚合层。{@code business_type} + {@code business_id} 关联到现有业务表
 * （outside_request / service_launch 等）；通用工单使用 {@code business_type=general}。
 *
 * <p>{@code dept_id} 走 {@code DeptDataPermissionRule} 自动 SQL 重写；{@code assignee_id}
 * 用于「我的工单」过滤（在 service 层显式跳过数据权限）。
 */
@TableName(value = "shuhe_ticket", autoResultMap = true)
@KeySequence("shuhe_ticket_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 工单编号，如 TK20260518001。
     */
    private String ticketNo;

    /**
     * 工单标题。
     */
    private String title;

    /**
     * 工单描述（富文本 HTML）。
     */
    private String content;

    /**
     * 工单分类 ID，关联 {@code shuhe_ticket_category}。
     */
    private Long categoryId;

    /**
     * 优先级，{@link cn.shuhe.system.module.ticket.enums.TicketPriorityEnum}。
     */
    private Integer priority;

    /**
     * 来源，{@link cn.shuhe.system.module.ticket.enums.TicketSourceEnum}。
     */
    private Integer source;

    /**
     * 业务类型，{@link cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum}。
     */
    private String businessType;

    /**
     * 关联业务表 ID（business_type 非 general 时使用）。
     */
    private Long businessId;

    /**
     * 关联 BPM 流程实例 ID（复杂工单挂流程时使用，预留）。
     */
    private String processInstanceId;

    /**
     * 状态，{@link cn.shuhe.system.module.ticket.enums.TicketStatusEnum}。
     */
    private Integer status;

    /**
     * 子状态（自由扩展）。
     */
    private String subStatus;

    /**
     * 提单人 ID。
     */
    private Long creatorId;

    /**
     * 提单人姓名快照。
     */
    private String creatorName;

    /** 项目经理/业务负责人 ID（创建工单时从项目负责人快照固化）。 */
    private Long projectManagerId;

    /** 项目经理姓名快照。 */
    private String projectManagerName;

    /**
     * 当前处理人 ID。
     */
    private Long assigneeId;

    /**
     * 处理人姓名快照。
     */
    private String assigneeName;

    /**
     * 处理人部门 ID（冗余存储，避免反复查询）。
     */
    private Long assigneeDeptId;

    /** 主执行人 ID。 */
    private Long primaryExecutorId;

    /** 主执行人姓名快照。 */
    private String primaryExecutorName;

    /** 技术审核人 ID。 */
    private Long techReviewerId;

    /** 技术审核人姓名快照。 */
    private String techReviewerName;

    /**
     * 工单归属部门 ID（{@code DeptDataPermissionRule} 自动过滤字段）。
     */
    private Long deptId;

    /**
     * 截止时间（SLA）。
     */
    private LocalDateTime dueTime;

    /**
     * 首次响应时间（assignee 接单时回写）。
     */
    private LocalDateTime firstResponseTime;

    /**
     * 完成时间（finish 动作时回写）。
     */
    private LocalDateTime finishTime;

    /**
     * 关闭时间（close 动作时回写）。
     */
    private LocalDateTime closeTime;

    /**
     * 验收人 ID（review_pass / review_reject 动作时回写）。
     */
    private Long reviewerId;

    /**
     * 验收人姓名快照。
     */
    private String reviewerName;

    /**
     * 验收时间。
     */
    private LocalDateTime reviewTime;

    /**
     * 验收意见（通过的评价或驳回原因）。
     */
    private String reviewComment;

    /**
     * 重开次数（reopen 动作累加，超上限禁止重开）。
     */
    private Integer reopenCount;

    /**
     * 拒单退回原因（return 动作时回写；resubmit 时清空）。
     */
    private String returnReason;

    /**
     * 通知通道，逗号分隔：{@code inner,dingtalk,sms,email}。
     */
    private String notifyChannels;

    /**
     * 通知状态：0=未通知 / 1=已通知 / 2=失败。
     */
    private Integer notifyStatus;

    /**
     * 关联项目 ID（可选）。
     */
    private Long projectId;

    /**
     * 对应的精确服务项 ID。新建服务工单必填；历史工单可为空。
     */
    private Long serviceItemId;

    /**
     * 关联客户 ID（可选）。
     */
    private Long customerId;

    /** 服务场景，如 penetration_test / vuln_scan / incident。 */
    private String serviceScene;

    /**
     * 扩展字段（JSON）。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extJson;

    /**
     * 备注。
     */
    private String remark;

}
