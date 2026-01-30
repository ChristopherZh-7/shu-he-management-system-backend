package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 项目工作记录分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectWorkRecordPageReqVO extends PageParam {

    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    @Schema(description = "项目类型: 1-安全服务 2-安全运营 3-数据安全", example = "1")
    private Integer projectType;

    @Schema(description = "服务项ID", example = "1")
    private Long serviceItemId;

    @Schema(description = "工作类型", example = "patrol")
    private String workType;

    @Schema(description = "记录人ID", example = "1")
    private Long creatorId;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "记录日期-开始", example = "2026-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate recordDateStart;

    @Schema(description = "记录日期-结束", example = "2026-01-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate recordDateEnd;

    @Schema(description = "关键字搜索（项目名称/工作内容）", example = "巡检")
    private String keyword;

    @Schema(description = "是否包含下属部门的数据", example = "true")
    private Boolean includeSubDept;

}
