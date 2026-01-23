package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 外出请求创建/更新 Request VO")
@Data
public class OutsideRequestSaveReqVO {

    @Schema(description = "主键ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "关联项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关联项目不能为空")
    private Long projectId;

    @Schema(description = "关联服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "目标部门ID（向哪个部门申请人员）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标部门不能为空")
    private Long targetDeptId;

    @Schema(description = "外出地点/客户现场", example = "XX银行总部")
    private String destination;

    @Schema(description = "外出事由", example = "协助进行渗透测试")
    private String reason;

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "外出人员ID列表（审批时由目标部门负责人选择）", example = "[1, 2, 3]")
    private List<Long> memberUserIds;

}
