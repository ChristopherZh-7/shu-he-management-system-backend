package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 服务执行申请 Response VO")
@Data
public class ServiceExecutionRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "服务项ID")
    private Long serviceItemId;

    @Schema(description = "服务项名称")
    private String serviceItemName;

    @Schema(description = "服务类型编码")
    private String serviceType;

    @Schema(description = "部门类型")
    private Integer deptType;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "发起人ID")
    private Long requestUserId;

    @Schema(description = "发起人姓名")
    private String requestUserName;

    @Schema(description = "发起人部门ID")
    private Long requestDeptId;

    @Schema(description = "发起人部门名称")
    private String requestDeptName;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "执行人ID列表")
    private List<Long> executorIds;

    @Schema(description = "执行人姓名列表")
    private String executorNames;

    @Schema(description = "状态：0待审批 1已通过 2已拒绝 3已取消")
    private Integer status;

    @Schema(description = "工作流流程实例ID")
    private String processInstanceId;

    @Schema(description = "创建的轮次ID")
    private Long roundId;

    @Schema(description = "备注")
    private String remark;

    // ========== 渗透测试附件 ==========

    @Schema(description = "授权书附件URL列表")
    private List<String> authorizationUrls;

    @Schema(description = "测试范围附件URL列表")
    private List<String> testScopeUrls;

    @Schema(description = "账号密码附件URL列表")
    private List<String> credentialsUrls;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
