package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 外出请求 Response VO")
@Data
public class OutsideRequestRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "关联项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long projectId;

    @Schema(description = "项目名称", example = "XX银行渗透测试项目")
    private String projectName;

    @Schema(description = "关联服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "服务项名称", example = "渗透测试")
    private String serviceItemName;

    @Schema(description = "服务类型编码", example = "penetration_test")
    private String serviceType;

    @Schema(description = "部门类型（1-安服 2-安运 3-数安）", example = "1")
    private Integer deptType;

    @Schema(description = "发起人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long requestUserId;

    @Schema(description = "发起人姓名", example = "张三")
    private String requestUserName;

    @Schema(description = "发起人部门ID", example = "1")
    private Long requestDeptId;

    @Schema(description = "发起人部门名称", example = "安全服务部")
    private String requestDeptName;

    @Schema(description = "目标部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long targetDeptId;

    @Schema(description = "目标部门名称", example = "安全运营部")
    private String targetDeptName;

    @Schema(description = "外出地点/客户现场", example = "XX银行总部")
    private String destination;

    @Schema(description = "外出事由", example = "协助进行渗透测试")
    private String reason;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "状态：0待审批 1已通过 2已拒绝 3已完成 4已取消", example = "1")
    private Integer status;

    @Schema(description = "工作流流程实例ID", example = "xxx-xxx-xxx")
    private String processInstanceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "外出人员列表")
    private List<OutsideMemberRespVO> members;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
