package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 部门服务单创建/更新 Request VO")
@Data
public class ProjectDeptServiceSaveReqVO {

    @Schema(description = "主键ID（更新时必填）", example = "1")
    private Long id;

    @Schema(description = "所属项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属项目ID不能为空")
    private Long projectId;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "部门类型不能为空")
    private Integer deptType;

    @Schema(description = "所属部门ID", example = "100")
    private Long deptId;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    // ========== 负责人 ==========

    @Schema(description = "负责人ID列表", example = "[1, 2]")
    private List<Long> managerIds;

    @Schema(description = "负责人姓名列表", example = "[\"张三\", \"李四\"]")
    private List<String> managerNames;

    // ========== 状态和进度 ==========

    @Schema(description = "状态：0待领取 1待开始 2进行中 3已暂停 4已完成 5已取消", example = "1")
    private Integer status;

    @Schema(description = "进度百分比 0-100", example = "50")
    private Integer progress;

    // ========== 时间信息 ==========

    @Schema(description = "计划开始时间")
    private LocalDateTime planStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime planEndTime;

    // ========== 扩展字段 ==========

    @Schema(description = "描述", example = "项目描述")
    private String description;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

}
