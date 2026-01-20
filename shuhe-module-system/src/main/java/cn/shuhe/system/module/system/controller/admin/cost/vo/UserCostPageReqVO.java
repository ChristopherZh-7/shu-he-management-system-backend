package cn.shuhe.system.module.system.controller.admin.cost.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理后台 - 用户成本分页查询 Request VO
 */
@Schema(description = "管理后台 - 用户成本分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserCostPageReqVO extends PageParam {

    @Schema(description = "用户昵称，模糊匹配", example = "张")
    private String nickname;

    @Schema(description = "部门编号", example = "100")
    private Long deptId;

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "职级，模糊匹配", example = "P2")
    private String positionLevel;

    @Schema(description = "计算年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer year;

    @Schema(description = "计算月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer month;

}
