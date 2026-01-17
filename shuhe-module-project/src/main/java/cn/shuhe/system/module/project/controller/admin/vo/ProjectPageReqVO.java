package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectPageReqVO extends PageParam {

    @Schema(description = "项目名称", example = "银行")
    private String name;

    @Schema(description = "项目编号", example = "PRJ-1")
    private String code;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "项目状态：0草稿 1进行中 2已完成", example = "1")
    private Integer status;

}
