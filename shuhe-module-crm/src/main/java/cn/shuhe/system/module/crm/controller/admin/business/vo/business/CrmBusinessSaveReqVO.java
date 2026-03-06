package cn.shuhe.system.module.crm.controller.admin.business.vo.business;

import cn.shuhe.system.module.crm.framework.operatelog.core.CrmCustomerParseFunction;
import cn.shuhe.system.module.crm.framework.operatelog.core.SysAdminUserParseFunction;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.shuhe.system.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 商机创建/更新 Request VO")
@Data
public class CrmBusinessSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "32129")
    private Long id;

    @Schema(description = "商机名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @DiffLogField(name = "商机名称")
    @NotNull(message = "商机名称不能为空")
    private String name;

    @Schema(description = "最终客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10299")
    @DiffLogField(name = "最终客户", function = CrmCustomerParseFunction.NAME)
    @NotNull(message = "最终客户不能为空")
    private Long customerId;

    @Schema(description = "合作商客户编号（可为空）", example = "10300")
    @DiffLogField(name = "合作商", function = CrmCustomerParseFunction.NAME)
    private Long intermediaryId;

    @Schema(description = "下次联系时间")
    @DiffLogField(name = "下次联系时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime contactNextTime;

    @Schema(description = "负责人用户编号", example = "14334")
    @NotNull(message = "负责人不能为空")
    @DiffLogField(name = "负责人", function = SysAdminUserParseFunction.NAME)
    private Long ownerUserId;

    @Schema(description = "部门金额分配列表")
    @NotEmpty(message = "部门分配不能为空")
    private List<DeptAllocation> deptAllocations;

    @Schema(description = "预计成交日期")
    @DiffLogField(name = "预计成交日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dealTime;

    @Schema(description = "预计合同总金额", example = "100000.00")
    @DiffLogField(name = "预计合同总金额")
    @NotNull(message = "预计合同总金额不能为空")
    private BigDecimal totalPrice;

    @Schema(description = "备注", example = "随便")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "联系人编号", example = "110")
    private Long contactId;

    @Schema(description = "部门金额分配")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptAllocation {

        @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "119")
        @NotNull(message = "部门编号不能为空")
        private Long deptId;

        @Schema(description = "部门名称", example = "安全服务部")
        private String deptName;

        @Schema(description = "分配金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "500000")
        @NotNull(message = "分配金额不能为空")
        private BigDecimal amount;
    }

}
