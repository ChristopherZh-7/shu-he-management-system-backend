package cn.shuhe.system.module.system.controller.admin.cost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 结算人选项 VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettleUserVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "是否为默认选项（发起人）")
    private Boolean isDefault;

    @Schema(description = "描述（如：发起人、XX部门主管）")
    private String description;
}
