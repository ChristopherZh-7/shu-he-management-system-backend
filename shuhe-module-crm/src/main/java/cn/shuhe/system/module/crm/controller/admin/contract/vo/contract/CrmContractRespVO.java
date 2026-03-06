package cn.shuhe.system.module.crm.controller.admin.contract.vo.contract;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 合同 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmContractRespVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10430")
    @ExcelProperty("合同编号")
    private Long id;

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("合同名称")
    private String name;

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "20230101")
    @ExcelProperty("合同编号")
    private String no;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @ExcelProperty("客户编号")
    private Long customerId;
    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "商机编号", example = "10864")
    @ExcelProperty("商机编号")
    private Long businessId;
    @Schema(description = "商机名称", example = "10864")
    @ExcelProperty("商机名称")
    private String businessName;

    @Schema(description = "合作商客户编号（可为空）", example = "10300")
    private Long intermediaryId;
    @Schema(description = "合作商名称", example = "XX科技有限公司")
    @ExcelProperty("合作商名称")
    private String intermediaryName;

    @Schema(description = "最后跟进时间")
    @ExcelProperty("最后跟进时间")
    private LocalDateTime contactLastTime;

    @Schema(description = "负责人的用户编号", example = "25682")
    @ExcelProperty("负责人的用户编号")
    private Long ownerUserId;
    @Schema(description = "负责人名字", example = "25682")
    @ExcelProperty("负责人名字")
    private String ownerUserName;
    @Schema(description = "负责人部门")
    @ExcelProperty("负责人部门")
    private String ownerUserDeptName;

    @Schema(description = "工作流编号", example = "1043")
    @ExcelProperty("工作流编号")
    private String processInstanceId;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("审批状态")
    private Integer auditStatus;

    @Schema(description = "下单日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("下单日期")
    private LocalDateTime orderDate;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "合同金额", example = "5617")
    @ExcelProperty("合同金额")
    private BigDecimal totalPrice;

    @Schema(description = "已回款金额", example = "5617")
    @ExcelProperty("已回款金额")
    private BigDecimal totalReceivablePrice;

    @Schema(description = "客户签约人编号", example = "18546")
    private Long signContactId;
    @Schema(description = "客户签约人", example = "小豆")
    @ExcelProperty("客户签约人")
    private String signContactName;

    @Schema(description = "公司签约人", example = "14036")
    private Long signUserId;
    @Schema(description = "公司签约人", example = "小明")
    @ExcelProperty("公司签约人")
    private String signUserName;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "合同附件URL")
    private String attachment;

    @Schema(description = "部门金额分配列表")
    private List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> deptAllocations;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人", example = "25682")
    @ExcelProperty("创建人")
    private String creator;

    @Schema(description = "创建人名字", example = "test")
    @ExcelProperty("创建人名字")
    private String creatorName;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
