package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 服务项批量创建 Request VO")
@Data
public class ServiceItemBatchSaveReqVO {

    @Schema(description = "所属项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属项目不能为空")
    private Long projectId;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "部门类型不能为空")
    private Integer deptType;

    @Schema(description = "服务项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "服务项列表不能为空")
    @Valid
    private List<ServiceItemSaveReqVO> items;

}
