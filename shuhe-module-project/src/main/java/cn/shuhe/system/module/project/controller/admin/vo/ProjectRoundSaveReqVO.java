package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 项目轮次创建/更新 Request VO")
@Data
public class ProjectRoundSaveReqVO {

    @Schema(description = "轮次ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目ID不能为空")
    private Long projectId;

    @Schema(description = "服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "轮次名称", example = "第1次渗透测试")
    private String name;

    @Schema(description = "截止日期（任务应在此日期前完成）")
    private LocalDateTime deadline;

    @Schema(description = "计划结束时间（保留字段）")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间（完成时间）")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "状态：0待执行 1执行中 2已完成 3已取消", example = "0")
    private Integer status;

    @Schema(description = "轮次序号", example = "1")
    private Integer roundNo;

    @Schema(description = "执行人ID列表", example = "[1, 2, 3]")
    private List<Long> executorIds;

    @Schema(description = "执行人姓名列表", example = "王五,李六")
    private String executorNames;

    @Schema(description = "备注")
    private String remark;

}
