package cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.shuhe.system.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉数据映射分页 Request VO")
@Data
public class DingtalkMappingPageReqVO extends PageParam {

    @Schema(description = "钉钉配置ID", example = "31125")
    private Long configId;

    @Schema(description = "类型（DEPT-部门，USER-用户）", example = "2")
    private String type;

    @Schema(description = "本地ID", example = "3825")
    private Long localId;

    @Schema(description = "钉钉ID", example = "10379")
    private String dingtalkId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}