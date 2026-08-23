package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 项目的部门服务包摘要")
@Data
public class ProjectDepartmentSummaryRespVO {

    @Schema(description = "部门服务单ID")
    private Long deptServiceId;

    @Schema(description = "负责部门ID")
    private Long deptId;

    @Schema(description = "负责部门名称")
    private String deptName;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全")
    private Integer deptType;

    @Schema(description = "部门服务单状态")
    private Integer status;

    @Schema(description = "根据服务项计算的进度")
    private Integer progress;

    @Schema(description = "进度加权分母（有限频次按计划次数，按需服务按 1）")
    private Integer progressWeight;

    @Schema(description = "合同计划执行次数（不包含按需服务）")
    private Integer plannedExecutionCount;

    @Schema(description = "已经提单人验收通过的执行次数")
    private Integer acceptedExecutionCount;

    @Schema(description = "是否包含按需服务")
    private Boolean hasOnDemandService;

    @Schema(description = "服务项总数")
    private Integer serviceItemCount;

    @Schema(description = "已完成服务项数")
    private Integer completedServiceItemCount;

    @Schema(description = "驻场服务项数")
    private Integer onsiteServiceItemCount;

    @Schema(description = "二线服务项数")
    private Integer remoteServiceItemCount;

    @Schema(description = "安全运营管理服务项数")
    private Integer managementServiceItemCount;

    @Schema(description = "当前用户是否可管理该部门服务包")
    private Boolean canManage;
}
