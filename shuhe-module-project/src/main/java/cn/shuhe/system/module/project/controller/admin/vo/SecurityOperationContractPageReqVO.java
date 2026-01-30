package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 安全运营合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SecurityOperationContractPageReqVO extends PageParam {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "状态：0-待启动 1-进行中 2-已结束 3-已终止")
    private Integer status;

}
