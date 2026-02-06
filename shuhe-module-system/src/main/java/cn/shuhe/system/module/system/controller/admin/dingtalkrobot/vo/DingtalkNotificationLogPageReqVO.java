package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import cn.shuhe.system.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉通知发送日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DingtalkNotificationLogPageReqVO extends PageParam {

    @Schema(description = "配置ID", example = "1")
    private Long configId;

    @Schema(description = "机器人ID", example = "1")
    private Long robotId;

    @Schema(description = "事件类型", example = "contract_create")
    private String eventType;

    @Schema(description = "事件模块", example = "crm")
    private String eventModule;

    @Schema(description = "业务ID", example = "1")
    private Long businessId;

    @Schema(description = "发送状态（0-成功 1-失败）", example = "0")
    private Integer sendStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
