package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外出请求分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OutsideRequestPageReqVO extends PageParam {

    @Schema(description = "关联项目ID", example = "1")
    private Long projectId;

    @Schema(description = "关联服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "发起人ID", example = "1")
    private Long requestUserId;

    @Schema(description = "目标部门ID", example = "1")
    private Long targetDeptId;

    @Schema(description = "外出地点", example = "客户现场")
    private String destination;

    @Schema(description = "状态：0待审批 1已通过 2已拒绝 3已完成 4已取消", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
