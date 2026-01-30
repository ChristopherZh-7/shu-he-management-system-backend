package cn.shuhe.system.module.system.controller.admin.cost.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台 - 合同部门分配分页 Request VO
 */
@Schema(description = "管理后台 - 合同部门分配分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDeptAllocationPageReqVO extends PageParam {

    @Schema(description = "CRM合同ID", example = "100")
    private Long contractId;

    @Schema(description = "部门ID", example = "101")
    private Long deptId;

    @Schema(description = "合同编号", example = "HT-2026")
    private String contractNo;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

}
