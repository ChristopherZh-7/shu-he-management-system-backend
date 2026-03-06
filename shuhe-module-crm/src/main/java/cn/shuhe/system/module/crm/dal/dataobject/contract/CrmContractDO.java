package cn.shuhe.system.module.crm.dal.dataobject.contract;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.dal.typehandler.DeptAllocationListTypeHandler;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM 合同 DO
 *
 * @author dhb52
 */
@TableName(value = "crm_contract", autoResultMap = true)
@KeySequence("crm_contract_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmContractDO extends BaseDO {

    /**
     * 合同编号
     */
    @TableId
    private Long id;
    /**
     * 合同名称
     */
    private String name;
    /**
     * 合同编号
     */
    private String no;
    /**
     * 客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 商机编号，非必须
     *
     * 关联 {@link CrmBusinessDO#getId()}
     */
    private Long businessId;

    /**
     * 合作商客户编号（可为空）
     * 从商机带入，模式一时（合作商介绍、合同签给最终客户）填合作商
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long intermediaryId;

    /**
     * 最后跟进时间
     */
    private LocalDateTime contactLastTime;

    /**
     * 负责人的用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long ownerUserId;

    /**
     * 工作流编号
     *
     * 关联 ProcessInstance 的 id 属性
     */
    private String processInstanceId;
    /**
     * 审批状态
     *
     * 枚举 {@link CrmAuditStatusEnum}
     */
    private Integer auditStatus;

    /**
     * 下单日期
     */
    private LocalDateTime orderDate;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 合同总金额，单位：元
     */
    private BigDecimal totalPrice;
    /**
     * 客户签约人，非必须
     *
     * 关联 {@link CrmContactDO#getId()}
     */
    private Long signContactId;
    /**
     * 公司签约人，非必须
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long signUserId;
    /**
     * 备注
     */
    private String remark;

    /**
     * 合同附件URL
     */
    private String attachment;

    /**
     * 部门金额分配列表（JSON），签合同时按部门重新分配合同金额
     * 格式: [{"deptId":119,"deptName":"安全服务部","amount":500000}, ...]
     */
    @TableField(typeHandler = DeptAllocationListTypeHandler.class)
    private List<CrmBusinessDO.DeptAllocation> deptAllocations;

}
