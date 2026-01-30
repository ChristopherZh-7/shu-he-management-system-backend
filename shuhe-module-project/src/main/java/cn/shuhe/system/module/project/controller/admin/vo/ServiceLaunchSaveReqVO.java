package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 统一服务发起创建/更新 Request VO")
@Data
public class ServiceLaunchSaveReqVO {

    @Schema(description = "ID，更新时必填")
    private Long id;

    @Schema(description = "关联合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联服务项ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "服务项ID不能为空")
    private Long serviceItemId;

    @Schema(description = "执行部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行部门ID不能为空")
    private Long executeDeptId;

    @Schema(description = "是否外出")
    private Boolean isOutside;

    @Schema(description = "是否跨部门")
    private Boolean isCrossDept;

    @Schema(description = "是否代发起")
    private Boolean isDelegation;

    @Schema(description = "被代发起人ID")
    private Long delegateUserId;

    @Schema(description = "外出地点")
    private String destination;

    @Schema(description = "外出事由")
    private String reason;

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

}
