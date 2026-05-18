package cn.shuhe.system.module.ticket.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketPageReqVO extends PageParam {

    @Schema(description = "工单编号（模糊）", example = "TK202605")
    private String ticketNo;

    @Schema(description = "工单标题（模糊）", example = "登录")
    private String title;

    @Schema(description = "工单分类 ID")
    private Long categoryId;

    @Schema(description = "优先级：0低 1中 2高 3紧急", example = "2")
    private Integer priority;

    @Schema(description = "状态：0待处理 1处理中 2待审核 3已完成 4已关闭 5已取消", example = "1")
    private Integer status;

    @Schema(description = "业务类型", example = "general")
    private String businessType;

    @Schema(description = "提单人 ID")
    private Long creatorId;

    @Schema(description = "处理人 ID")
    private Long assigneeId;

    @Schema(description = "归属部门 ID")
    private Long deptId;

    @Schema(description = "创建时间区间 [start, end]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "截止时间区间 [start, end]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] dueTime;

}
