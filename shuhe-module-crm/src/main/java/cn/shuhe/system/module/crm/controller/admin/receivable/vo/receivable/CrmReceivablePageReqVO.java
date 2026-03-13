package cn.shuhe.system.module.crm.controller.admin.receivable.vo.receivable;

import cn.shuhe.system.framework.common.pojo.PageParam;
import cn.shuhe.system.framework.common.validation.InEnum;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 回款分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmReceivablePageReqVO extends PageParam {

    @Schema(description = "回款编号")
    private String no;

    @Schema(description = "回款计划编号", example = "31177")
    private Long planId;

    @Schema(description = "客户编号", example = "4963")
    private Long customerId;

    @Schema(description = "合同编号", example = "4963")
    private Long contractId;

    @Schema(description = "场景类型：1-我负责的，2-我参与的，3-下属负责的；null 表示全部", example = "1")
    @Min(value = 1, message = "场景类型必须在 1-3 之间")
    @Max(value = 3, message = "场景类型必须在 1-3 之间")
    private Integer sceneType; // 场景类型，为 null 时则表示全部

    @Schema(description = "审批状态", example = "20")
    @InEnum(CrmAuditStatusEnum.class)
    private Integer auditStatus;

}
