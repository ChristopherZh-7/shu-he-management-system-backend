package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 项目工作记录新增/修改 Request VO")
@Data
public class ProjectWorkRecordSaveReqVO {

    @Schema(description = "记录ID，新增时不传", example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目不能为空")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目类型不能为空")
    private Integer projectType;

    @Schema(description = "项目名称", example = "XX银行安保项目")
    private String projectName;

    @Schema(description = "服务项ID（可选）", example = "1")
    private Long serviceItemId;

    @Schema(description = "服务项名称", example = "日常巡检")
    private String serviceItemName;

    @Schema(description = "记录日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-28")
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @Schema(description = "工作类型: patrol-巡检, meeting-会议, report-报告, incident-事件处理, training-培训, maintenance-维护, other-其他", example = "patrol")
    private String workType;

    @Schema(description = "工作内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "完成现场巡检，发现3处安全隐患并已处理")
    @NotBlank(message = "工作内容不能为空")
    private String workContent;

    @Schema(description = "附件URL列表", example = "[\"https://xxx.com/file1.pdf\"]")
    private List<String> attachments;

    @Schema(description = "备注", example = "无")
    private String remark;

}
