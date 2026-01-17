package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 服务项导入响应 VO")
@Data
@Builder
public class ServiceItemImportRespVO {

    @Schema(description = "创建成功数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer createCount;

    @Schema(description = "导入失败数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer failureCount;

    @Schema(description = "导入失败的记录及原因")
    private Map<Integer, String> failureRecords;

}
