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

    @Schema(description = "轮次名称", example = "第1次渗透测试")
    private String name;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "执行人ID列表", example = "[1, 2, 3]")
    private List<Long> executorIds;

    @Schema(description = "执行人姓名列表", example = "王五,李六")
    private String executorNames;

    @Schema(description = "备注")
    private String remark;

}
