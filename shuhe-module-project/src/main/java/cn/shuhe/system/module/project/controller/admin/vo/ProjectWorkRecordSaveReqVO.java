package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 项目工作记录新增/修改 Request VO")
@Data
public class ProjectWorkRecordSaveReqVO {

    @Schema(description = "记录ID，新增时不传", example = "1")
    private Long id;

    @Schema(description = "项目ID，内部工作可为空", example = "1")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全，内部工作可为空", example = "1")
    private Integer projectType;

    @Schema(description = "项目名称", example = "XX银行安保项目")
    private String projectName;

    @Schema(description = "服务项ID（可选）", example = "1")
    private Long serviceItemId;

    @Schema(description = "任务来源类型: internal-内部工作 manual-项目手工 service_item-固定服务项 round-项目轮次 ticket-服务工单", example = "round")
    @Pattern(regexp = "internal|manual|service_item|round|ticket", message = "任务来源类型不合法")
    private String sourceType;

    @Schema(description = "任务来源ID（轮次/工单/驻场记录ID）", example = "1")
    private Long sourceId;

    @Schema(description = "记录日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-28")
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @Schema(description = "工作类型: patrol-巡检, meeting-会议, report-报告, incident-事件处理, training-培训, maintenance-维护, other-其他", example = "patrol")
    private String workType;

    @Schema(description = "工作内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "完成现场巡检，发现3处安全隐患并已处理")
    @NotBlank(message = "工作内容不能为空")
    private String workContent;

    @Schema(description = "个人实际投入分钟数", example = "180")
    @NotNull(message = "请填写实际投入时长")
    @Min(value = 1, message = "实际投入不能少于1分钟")
    @Max(value = 1440, message = "单条记录实际投入不能超过24小时")
    private Integer actualMinutes;

    @Schema(description = "本条工作完成比例0-100", example = "60")
    @Min(value = 0, message = "完成比例不能小于0")
    @Max(value = 100, message = "完成比例不能大于100")
    private Integer completionPercent;

    @Schema(description = "工作结果/产出说明", example = "完成20个目标探测，确认1个高危漏洞")
    private String workResult;

    @Schema(description = "产出数量", example = "20")
    @DecimalMin(value = "0", message = "产出数量不能小于0")
    private BigDecimal outputQuantity;

    @Schema(description = "产出单位", example = "台")
    @Size(max = 32, message = "产出单位不能超过32个字符")
    private String outputUnit;

    @Schema(description = "附件URL列表", example = "[\"https://xxx.com/file1.pdf\"]")
    private List<String> attachments;

    @Schema(description = "备注", example = "无")
    private String remark;

}
