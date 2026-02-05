package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import lombok.*;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.shuhe.system.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉群机器人消息记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DingtalkRobotMessagePageReqVO extends PageParam {

    @Schema(description = "机器人编号", example = "1")
    private Long robotId;

    @Schema(description = "消息类型（text/markdown/actionCard/link）", example = "text")
    private String msgType;

    @Schema(description = "发送状态（0-成功 1-失败）", example = "0")
    private Integer sendStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
