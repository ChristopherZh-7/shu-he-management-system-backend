package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 服务项分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceItemPageReqVO extends PageParam {

    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    @Schema(description = "服务项名称", example = "渗透测试")
    private String name;

    @Schema(description = "服务项编号", example = "PRJ-1")
    private String code;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "服务类型", example = "penetration_test")
    private String serviceType;

    @Schema(description = "服务项状态：0草稿 1进行中 2已暂停 3已完成 4已取消", example = "1")
    private Integer status;

    @Schema(description = "优先级：0低 1中 2高", example = "1")
    private Integer priority;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "所属部门ID", example = "1")
    private Long deptId;

}
