package cn.shuhe.system.module.ticket.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketPageReqVO extends PageParam {

    @Schema(description = "工单编号", example = "TK202601150001")
    private String ticketNo;

    @Schema(description = "工单标题", example = "网络故障")
    private String title;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Schema(description = "优先级", example = "1")
    private Integer priority;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "处理人ID", example = "1")
    private Long assigneeId;

    @Schema(description = "创建人ID", example = "1")
    private Long creatorId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
