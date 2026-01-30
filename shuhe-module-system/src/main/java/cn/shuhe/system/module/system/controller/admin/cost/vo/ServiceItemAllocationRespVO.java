package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 服务项金额分配 Response VO
 */
@Schema(description = "管理后台 - 服务项金额分配 Response VO")
@Data
public class ServiceItemAllocationRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "合同部门分配ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long contractDeptAllocationId;

    @Schema(description = "分配类型：service_item-服务项分配, so_management-安全运营管理费, so_onsite-安全运营驻场费", example = "service_item")
    private String allocationType;

    @Schema(description = "服务项ID（服务项分配时有值）", example = "200")
    private Long serviceItemId;

    @Schema(description = "服务项名称（安全运营分配时为管理费/驻场费）", example = "渗透测试")
    private String serviceItemName;

    @Schema(description = "服务项编号", example = "SVC-2026-001")
    private String serviceItemCode;

    @Schema(description = "服务类型", example = "penetration_test")
    private String serviceType;

    @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50000.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "备注", example = "渗透测试服务费")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
