package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 工单 Response VO（详情 + 列表共用，列表查询时 _actions / commentCount / attachmentCount 可能为 null）")
@Data
public class TicketRespVO {

    @Schema(description = "工单 ID")
    private Long id;

    @Schema(description = "工单编号")
    private String ticketNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述（富文本 HTML）")
    private String content;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名称（展开）")
    private String categoryName;

    @Schema(description = "优先级 0/1/2/3")
    private Integer priority;

    @Schema(description = "来源 0手动 1外协 2服务派遣 3API")
    private Integer source;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "关联业务 ID")
    private Long businessId;

    @Schema(description = "BPM 流程实例 ID")
    private String processInstanceId;

    @Schema(description = "状态码 0~6")
    private Integer status;

    @Schema(description = "状态中文（展开）")
    private String statusName;

    @Schema(description = "子状态")
    private String subStatus;

    @Schema(description = "提单人 ID")
    private Long creatorId;

    @Schema(description = "提单人姓名快照")
    private String creatorName;

    @Schema(description = "处理人 ID")
    private Long assigneeId;

    @Schema(description = "处理人姓名快照")
    private String assigneeName;

    @Schema(description = "处理人部门 ID")
    private Long assigneeDeptId;

    @Schema(description = "处理人部门名（展开）")
    private String assigneeDeptName;

    @Schema(description = "实际执行人姓名列表（详情查询返回）")
    private List<String> executorNames;

    @Schema(description = "归属部门 ID")
    private Long deptId;

    @Schema(description = "归属部门名（展开）")
    private String deptName;

    @Schema(description = "截止时间")
    private LocalDateTime dueTime;

    @Schema(description = "首次响应时间")
    private LocalDateTime firstResponseTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "关闭时间")
    private LocalDateTime closeTime;

    @Schema(description = "验收人 ID")
    private Long reviewerId;

    @Schema(description = "验收人姓名快照")
    private String reviewerName;

    @Schema(description = "验收时间")
    private LocalDateTime reviewTime;

    @Schema(description = "验收意见（通过评价 / 驳回原因）")
    private String reviewComment;

    @Schema(description = "重开次数")
    private Integer reopenCount;

    @Schema(description = "拒单退回原因")
    private String returnReason;

    @Schema(description = "通知通道")
    private String notifyChannels;

    @Schema(description = "通知状态 0未通知 1已通知 2失败")
    private Integer notifyStatus;

    @Schema(description = "关联项目 ID")
    private Long projectId;

    @Schema(description = "关联的精确服务项 ID")
    private Long serviceItemId;

    @Schema(description = "项目名称（服务上下文快照）")
    private String projectName;

    @Schema(description = "服务项编号（服务上下文快照）")
    private String serviceItemCode;

    @Schema(description = "服务类型中文名（服务上下文快照）")
    private String serviceTypeName;

    @Schema(description = "服务模式：1 驻场 / 2 二线")
    private Integer serviceMode;

    @Schema(description = "服务来源：signed_contract / approved_early_investment")
    private String serviceSourceType;

    @Schema(description = "客户名称（服务上下文快照）")
    private String customerName;

    @Schema(description = "合同编号（提前投入可为空）")
    private String contractNo;

    @Schema(description = "关联客户 ID")
    private Long customerId;

    @Schema(description = "扩展字段")
    private Map<String, Object> extJson;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "评论数（仅详情查询返回）")
    private Integer commentCount;

    @Schema(description = "附件数（仅详情查询返回）")
    private Integer attachmentCount;

    @Schema(description = "当前用户可执行的 action 列表（仅详情查询返回）")
    private List<String> actions;

    @Schema(description = "是否外出工单（从 extJson.isOutside 提取，列表/详情都返回）")
    private Boolean outside;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
