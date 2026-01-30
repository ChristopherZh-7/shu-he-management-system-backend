package cn.shuhe.system.module.project.controller.admin.vo;

import cn.shuhe.system.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 部门服务单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProjectDeptServicePageReqVO extends PageParam {

    @Schema(description = "部门类型：1安全服务 2安全运营 3数据安全", example = "1")
    private Integer deptType;

    @Schema(description = "状态：0待领取 1待开始 2进行中 3已暂停 4已完成 5已取消", example = "1")
    private Integer status;

    @Schema(description = "是否已领取", example = "true")
    private Boolean claimed;

    @Schema(description = "客户名称", example = "某银行")
    private String customerName;

    @Schema(description = "合同编号", example = "HT-2026-001")
    private String contractNo;

    @Schema(description = "项目ID", example = "1")
    private Long projectId;

}
