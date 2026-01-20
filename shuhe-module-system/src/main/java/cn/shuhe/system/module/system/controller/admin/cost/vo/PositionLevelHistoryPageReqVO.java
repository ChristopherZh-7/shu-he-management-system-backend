package cn.shuhe.system.module.system.controller.admin.cost.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 管理后台 - 职级变更记录分页查询 Request VO
 */
@Schema(description = "管理后台 - 职级变更记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PositionLevelHistoryPageReqVO extends PageParam {

    @Schema(description = "用户ID", example = "100")
    private Long userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "变更类型：1自动同步 2手动录入", example = "1")
    private Integer changeType;

    @Schema(description = "生效日期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "生效日期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

}
