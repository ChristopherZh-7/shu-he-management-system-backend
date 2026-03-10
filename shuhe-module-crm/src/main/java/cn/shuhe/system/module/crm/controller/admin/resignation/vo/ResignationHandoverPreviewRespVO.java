package cn.shuhe.system.module.crm.controller.admin.resignation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 离职交接 - 预览 Response VO
 *
 * @author ShuHe
 */
@Schema(description = "管理后台 - 离职交接预览 Response VO")
@Data
@Builder
public class ResignationHandoverPreviewRespVO {

    @Schema(description = "客户数量", example = "12")
    private Integer customerCount;

    @Schema(description = "商机数量", example = "5")
    private Integer businessCount;

    @Schema(description = "合同数量", example = "8")
    private Integer contractCount;

    @Schema(description = "线索数量", example = "3")
    private Integer clueCount;

    @Schema(description = "项目数量（负责人包含离职用户）", example = "3")
    private Integer projectCount;

    @Schema(description = "部门服务单数量", example = "4")
    private Integer deptServiceCount;

    @Schema(description = "驻场负责人数量（is_leader=1）", example = "2")
    private Integer siteLeaderCount;
}
