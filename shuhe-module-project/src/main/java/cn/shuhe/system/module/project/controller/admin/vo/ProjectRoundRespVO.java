package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 项目轮次 Response VO")
@Data
public class ProjectRoundRespVO {

    @Schema(description = "轮次ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "轮次序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer roundNo;

    @Schema(description = "轮次名称", example = "第1次渗透测试")
    private String name;

    // ========== 时间信息 ==========

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    // ========== 执行信息（支持多人）==========

    @Schema(description = "执行人ID列表", example = "[1, 2, 3]")
    private List<Long> executorIds;

    @Schema(description = "执行人姓名列表", example = "王五,李六")
    private String executorNames;

    @Schema(description = "状态：0待执行 1执行中 2已完成 3已取消", example = "1")
    private Integer status;

    @Schema(description = "进度 0-100", example = "50")
    private Integer progress;

    // ========== 结果信息 ==========

    @Schema(description = "执行结果/报告摘要")
    private String result;

    @Schema(description = "附件")
    private String attachments;

    @Schema(description = "备注")
    private String remark;

    // ========== 渗透测试附件（来自服务执行申请）==========

    @Schema(description = "授权书附件URL列表")
    private List<String> authorizationUrls;

    @Schema(description = "测试范围附件URL列表")
    private List<String> testScopeUrls;

    @Schema(description = "账号密码附件URL列表")
    private List<String> credentialsUrls;

    // ========== 通用字段 ==========

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
