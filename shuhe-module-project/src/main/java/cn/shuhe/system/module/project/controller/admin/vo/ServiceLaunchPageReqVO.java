package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 统一服务发起分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceLaunchPageReqVO extends PageParam {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "服务项ID")
    private Long serviceItemId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否外出")
    private Boolean isOutside;

    @Schema(description = "是否跨部门")
    private Boolean isCrossDept;

}
