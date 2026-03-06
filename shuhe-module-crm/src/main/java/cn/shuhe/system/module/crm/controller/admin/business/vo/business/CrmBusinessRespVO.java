package cn.shuhe.system.module.crm.controller.admin.business.vo.business;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 商机 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CrmBusinessRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "32129")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "商机名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("商机名称")
    private String name;

    @Schema(description = "最终客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10299")
    private Long customerId;
    @Schema(description = "最终客户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("最终客户名称")
    private String customerName;

    @Schema(description = "合作商客户编号（可为空）", example = "10300")
    private Long intermediaryId;
    @Schema(description = "合作商名称", example = "XX科技有限公司")
    @ExcelProperty("合作商名称")
    private String intermediaryName;

    @Schema(description = "跟进状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @ExcelProperty("跟进状态")
    private Boolean followUpStatus;

    @Schema(description = "最后跟进时间")
    @ExcelProperty("最后跟进时间")
    private LocalDateTime contactLastTime;

    @Schema(description = "下次联系时间")
    @ExcelProperty("下次联系时间")
    private LocalDateTime contactNextTime;

    @Schema(description = "负责人的用户编号", example = "25682")
    @ExcelProperty("负责人的用户编号")
    private Long ownerUserId;
    @Schema(description = "负责人名字", example = "25682")
    @ExcelProperty("负责人名字")
    private String ownerUserName;
    @Schema(description = "负责人部门")
    @ExcelProperty("负责人部门")
    private String ownerUserDeptName;

    @Schema(description = "部门金额分配列表")
    private List<DeptAllocationVO> deptAllocations;

    @Data
    @Schema(description = "部门金额分配（含负责人）")
    public static class DeptAllocationVO {
        @Schema(description = "部门编号")
        private Long deptId;
        @Schema(description = "部门名称")
        private String deptName;
        @Schema(description = "分配金额")
        private java.math.BigDecimal amount;
        @Schema(description = "部门负责人姓名")
        private String deptLeaderName;
    }

    @Schema(description = "审批状态", example = "0")
    @ExcelProperty("审批状态")
    private Integer auditStatus;

    @Schema(description = "钉钉群会话ID")
    private String dingtalkChatId;

    @Schema(description = "结束状态", example = "1")
    @ExcelProperty("结束状态")
    private Integer endStatus;

    @ExcelProperty("结束时的备注")
    private String endRemark;

    @Schema(description = "预计成交日期")
    @ExcelProperty("预计成交日期")
    private LocalDateTime dealTime;

    @Schema(description = "预计合同总金额", example = "100000.00")
    @ExcelProperty("预计合同总金额")
    private BigDecimal totalPrice;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "提前投入审批状态（null=未发起，0=草稿，10=审批中，20=通过，30=驳回）", example = "20")
    private Integer earlyInvestmentStatus;

    @Schema(description = "提前投入 BPM 流程实例编号")
    private String earlyInvestmentProcessInstanceId;

    @Schema(description = "提前投入 - 投入人员列表")
    private List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.Personnel> earlyInvestmentPersonnel;

    @Schema(description = "提前投入 - 预计自垫资金（元）")
    private java.math.BigDecimal earlyInvestmentEstimatedCost;

    @Schema(description = "提前投入 - 工作内容")
    private String earlyInvestmentWorkScope;

    @Schema(description = "提前投入 - 计划开始日期")
    private java.time.LocalDate earlyInvestmentPlanStart;

    @Schema(description = "提前投入 - 计划结束日期")
    private java.time.LocalDate earlyInvestmentPlanEnd;

    @Schema(description = "提前投入 - 若合同未签的处理方式")
    private String earlyInvestmentRiskHandling;

    @Schema(description = "提前投入 - 申请理由")
    private String earlyInvestmentReason;

    @Schema(description = "创建人", example = "1024")
    @ExcelProperty("创建人")
    private String creator;
    @Schema(description = "创建人名字", example = "戍合")
    @ExcelProperty("创建人名字")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}
