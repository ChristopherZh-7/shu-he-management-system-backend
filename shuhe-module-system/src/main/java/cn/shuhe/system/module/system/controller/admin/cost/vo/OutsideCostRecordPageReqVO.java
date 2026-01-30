package cn.shuhe.system.module.system.controller.admin.cost.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 外出费用记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class OutsideCostRecordPageReqVO extends PageParam {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "目标部门ID")
    private Long targetDeptId;

    @Schema(description = "状态：0-待填写 1-已填写")
    private Integer status;
}
