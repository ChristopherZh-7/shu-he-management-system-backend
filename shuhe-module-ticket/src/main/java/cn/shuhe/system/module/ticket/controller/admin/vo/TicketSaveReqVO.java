package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 工单保存 Request VO（create + update 共用）")
@Data
public class TicketSaveReqVO {

    @Schema(description = "工单 ID，新增时不传", example = "1024")
    private Long id;

    @Schema(description = "工单标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "登录页提交按钮无响应")
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 200, message = "工单标题不能超过 200 个字符")
    private String title;

    @Schema(description = "工单描述（富文本 HTML）")
    private String content;

    @Schema(description = "工单分类 ID", example = "10")
    private Long categoryId;

    @Schema(description = "优先级：0低 1中 2高 3紧急", example = "1")
    private Integer priority;

    @Schema(description = "业务类型，默认 general（一期前端创建仅允许 general）", example = "general")
    private String businessType;

    @Schema(description = "关联业务 ID（business_type 非 general 时必填）")
    private Long businessId;

    @Schema(description = "工单归属部门 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101")
    @NotNull(message = "工单归属部门不能为空")
    private Long deptId;

    @Schema(description = "截止时间（SLA）")
    private LocalDateTime dueTime;

    @Schema(description = "关联项目 ID")
    private Long projectId;

    @Schema(description = "关联客户 ID")
    private Long customerId;

    @Schema(description = "扩展字段")
    private Map<String, Object> extJson;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;

}
