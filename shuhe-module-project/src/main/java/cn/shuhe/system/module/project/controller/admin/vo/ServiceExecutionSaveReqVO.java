package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 服务执行申请创建/更新 Request VO")
@Data
public class ServiceExecutionSaveReqVO {

    @Schema(description = "主键ID（更新时必填）")
    private Long id;

    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目不能为空")
    private Long projectId;

    @Schema(description = "服务项ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "服务项不能为空")
    private Long serviceItemId;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "备注")
    private String remark;

    // ========== 渗透测试附件 ==========

    @Schema(description = "授权书附件URL列表")
    private List<String> authorizationUrls;

    @Schema(description = "测试范围附件URL列表")
    private List<String> testScopeUrls;

    @Schema(description = "账号密码附件URL列表")
    private List<String> credentialsUrls;

    // ========== 审批时设置的字段 ==========

    @Schema(description = "执行人ID列表（审批时选择）")
    private List<Long> executorIds;

}
