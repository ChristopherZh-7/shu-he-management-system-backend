package cn.shuhe.system.module.crm.dal.dataobject.business;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.dal.typehandler.DeptAllocationListTypeHandler;
import cn.shuhe.system.module.crm.dal.typehandler.PersonnelListTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM 商机 DO
 *
 * @author ljlleo
 */
@TableName(value = "crm_business", autoResultMap = true)
@KeySequence("crm_business_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmBusinessDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 商机名称
     */
    private String name;
    /**
     * 最终客户编号（实际购买方）
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;

    /**
     * 合作商客户编号（可为空）
     * 模式一：有合作商介绍，此处填合作商客户
     * 模式二：直接对接最终客户，此处为空
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long intermediaryId;

    /**
     * 跟进状态
     */
    private Boolean followUpStatus;
    /**
     * 最后跟进时间
     */
    private LocalDateTime contactLastTime;
    /**
     * 下次联系时间
     */
    private LocalDateTime contactNextTime;

    /**
     * 负责人的用户编号
     */
    private Long ownerUserId;

    /**
     * 部门金额分配列表（JSON）
     * 格式: [{"deptId":119,"deptName":"安全服务部","amount":500000}, ...]
     */
    @TableField(typeHandler = DeptAllocationListTypeHandler.class)
    private List<DeptAllocation> deptAllocations;

    /**
     * 审批状态
     *
     * 枚举 {@link CrmAuditStatusEnum}
     */
    private Integer auditStatus;
    /**
     * BPM 流程实例编号
     */
    private String processInstanceId;
    /**
     * 钉钉群会话ID
     */
    private String dingtalkChatId;

    /**
     * 结束状态
     *
     * 枚举 {@link CrmBusinessEndStatusEnum}
     */
    private Integer endStatus;
    /**
     * 结束时的备注
     */
    private String endRemark;

    /**
     * 预计成交日期
     */
    private LocalDateTime dealTime;
    /**
     * 预计合同总金额，单位：元
     */
    private BigDecimal totalPrice;
    /**
     * 备注
     */
    private String remark;

    /**
     * 提前投入审批状态（null=未发起，0=草稿，10=审批中，20=已通过，30=已驳回）
     *
     * 枚举 {@link CrmAuditStatusEnum}
     */
    private Integer earlyInvestmentStatus;
    /**
     * 提前投入 BPM 流程实例编号
     */
    private String earlyInvestmentProcessInstanceId;

    /** 提前投入 - 投入人员列表（JSON） */
    @TableField(typeHandler = PersonnelListTypeHandler.class)
    private List<Personnel> earlyInvestmentPersonnel;
    /** 提前投入 - 预计自垫资金（元） */
    private BigDecimal earlyInvestmentEstimatedCost;
    /** 提前投入 - 工作内容 */
    private String earlyInvestmentWorkScope;
    /** 提前投入 - 计划开始日期 */
    private LocalDate earlyInvestmentPlanStart;
    /** 提前投入 - 计划结束日期 */
    private LocalDate earlyInvestmentPlanEnd;
    /** 提前投入 - 若合同未签的处理方式 */
    private String earlyInvestmentRiskHandling;
    /** 提前投入 - 申请理由 */
    private String earlyInvestmentReason;

    /**
     * 部门金额分配
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptAllocation {
        private Long deptId;
        private String deptName;
        private BigDecimal amount;
    }

    /**
     * 提前投入人员
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Personnel {
        /** 用户编号 */
        private Long userId;
        /** 用户姓名 */
        private String userName;
        /** 预计投入工时（天） */
        private Integer workDays;
    }

}
