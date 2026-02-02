package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 项目工作记录 Response VO")
@Data
public class ProjectWorkRecordRespVO {

    @Schema(description = "记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer projectType;

    @Schema(description = "项目名称", example = "XX银行安保项目")
    private String projectName;

    @Schema(description = "服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "服务项名称", example = "日常巡检")
    private String serviceItemName;

    @Schema(description = "服务类型（字典值）", example = "penetration_test")
    private String serviceType;

    @Schema(description = "记录日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-28")
    private LocalDate recordDate;

    @Schema(description = "工作类型", example = "patrol")
    private String workType;

    @Schema(description = "工作内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "完成现场巡检")
    private String workContent;

    @Schema(description = "附件URL列表", example = "[\"https://xxx.com/file1.pdf\"]")
    private List<String> attachments;

    @Schema(description = "备注", example = "无")
    private String remark;

    @Schema(description = "记录人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String creator;

    @Schema(description = "记录人姓名", example = "张三")
    private String creatorName;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
