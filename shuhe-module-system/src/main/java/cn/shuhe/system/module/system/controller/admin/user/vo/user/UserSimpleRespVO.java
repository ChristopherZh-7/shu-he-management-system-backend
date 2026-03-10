package cn.shuhe.system.module.system.controller.admin.user.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 用户精简信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "戍合")
    private String nickname;

    @Schema(description = "部门ID", example = "我是一个用户")
    private Long deptId;
    @Schema(description = "部门名称", example = "IT 部")
    private String deptName;

    @Schema(description = "离职日期", example = "2026-03-10")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDateTime resignDate;

}
