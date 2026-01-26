package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 服务执行申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceExecutionPageReqVO extends PageParam {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "服务项ID")
    private Long serviceItemId;

    @Schema(description = "发起人ID")
    private Long requestUserId;

    @Schema(description = "状态：0待审批 1已通过 2已拒绝 3已取消")
    private Integer status;

}
