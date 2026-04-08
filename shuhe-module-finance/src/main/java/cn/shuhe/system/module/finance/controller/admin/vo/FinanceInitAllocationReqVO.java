package cn.shuhe.system.module.finance.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 从合同初始化分配 Request VO")
@Data
public class FinanceInitAllocationReqVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "部门分配项")
    @NotNull(message = "分配项不能为空")
    private List<DeptAllocationItem> items;

    @Data
    public static class DeptAllocationItem {
        @Schema(description = "部门ID")
        private Long deptId;
        @Schema(description = "部门名称")
        private String deptName;
        @Schema(description = "分配金额")
        private BigDecimal amount;
    }

}
