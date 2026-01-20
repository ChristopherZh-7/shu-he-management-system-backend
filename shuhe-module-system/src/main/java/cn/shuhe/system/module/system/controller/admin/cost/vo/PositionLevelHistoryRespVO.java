package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台 - 职级变更记录 Response VO
 */
@Schema(description = "管理后台 - 职级变更记录 Response VO")
@Data
public class PositionLevelHistoryRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "用户账号", example = "zhangsan")
    private String username;

    @Schema(description = "部门名称", example = "安全服务部")
    private String deptName;

    @Schema(description = "变更前职级", example = "P1-1")
    private String oldPositionLevel;

    @Schema(description = "变更后职级", requiredMode = Schema.RequiredMode.REQUIRED, example = "P2-1")
    private String newPositionLevel;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-15")
    private LocalDate effectiveDate;

    @Schema(description = "变更类型：1自动同步 2手动录入", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer changeType;

    @Schema(description = "变更类型名称", example = "自动同步")
    private String changeTypeName;

    @Schema(description = "备注", example = "年度晋升")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
