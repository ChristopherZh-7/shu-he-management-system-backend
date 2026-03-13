package cn.shuhe.system.module.crm.controller.admin.clue.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 线索分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmCluePageReqVO extends PageParam {

    @Schema(description = "线索名称", example = "线索xxx")
    private String name;

    @Schema(description = "转化状态", example = "2048")
    private Boolean transformStatus;

    @Schema(description = "电话", example = "18000000000")
    private String telephone;

    @Schema(description = "手机号", example = "18000000000")
    private String mobile;

    @Schema(description = "场景类型：1-我负责的，2-我参与的，3-下属负责的；null 表示全部", example = "1")
    @Min(value = 1, message = "场景类型必须在 1-3 之间")
    @Max(value = 3, message = "场景类型必须在 1-3 之间")
    private Integer sceneType; // 场景类型，为 null 时则表示全部

    @Schema(description = "所属行业", example = "1")
    private Integer industryId;

    @Schema(description = "客户等级", example = "1")
    private Integer level;

    @Schema(description = "客户来源", example = "1")
    private Integer source;

    @Schema(description = "跟进状态", example = "true")
    private Boolean followUpStatus;

    @Schema(description = "创建时间", example = "[2023-01-01 00:00:00, 2023-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
