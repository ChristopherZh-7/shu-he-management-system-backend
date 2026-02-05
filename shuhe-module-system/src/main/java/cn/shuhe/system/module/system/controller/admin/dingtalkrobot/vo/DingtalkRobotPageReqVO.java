package cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo;

import lombok.*;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.shuhe.system.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉群机器人分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DingtalkRobotPageReqVO extends PageParam {

    @Schema(description = "机器人名称", example = "项目通知机器人")
    private String name;

    @Schema(description = "状态（0-正常 1-停用）", example = "0")
    private Integer status;

    @Schema(description = "安全类型（1-关键词 2-加签 3-IP白名单）", example = "2")
    private Integer securityType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
