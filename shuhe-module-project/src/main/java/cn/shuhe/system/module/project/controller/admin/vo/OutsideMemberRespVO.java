package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外出人员 Response VO")
@Data
public class OutsideMemberRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "外出请求ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long requestId;

    @Schema(description = "外出人员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "人员姓名", example = "李四")
    private String userName;

    @Schema(description = "人员部门ID", example = "2")
    private Long userDeptId;

    @Schema(description = "部门名称", example = "安全运营部")
    private String userDeptName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
