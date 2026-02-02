package cn.shuhe.system.module.system.dal.dataobject.cost;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 合同部门分配 DO
 * 
 * 记录合同金额分配到各部门的情况（第一级分配）
 */
@TableName("contract_dept_allocation")
@KeySequence("contract_dept_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDeptAllocationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * CRM合同ID
     */
    private Long contractId;

    /**
     * 合同编号（冗余，方便查询展示）
     */
    private String contractNo;

    /**
     * 客户名称（冗余，方便查询展示）
     */
    private String customerName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称（冗余，方便查询展示）
     */
    private String deptName;

    /**
     * 上级分配ID（NULL表示从合同直接分配的第一级）
     */
    private Long parentAllocationId;

    /**
     * 分配层级（1=一级部门, 2=二级, 以此类推）
     */
    private Integer allocationLevel;

    /**
     * 从上级获得的金额
     */
    private BigDecimal receivedAmount;

    /**
     * 已分配给下级的金额
     */
    private BigDecimal distributedAmount;

    /**
     * 分配金额（等于receivedAmount，保持向后兼容）
     */
    private BigDecimal allocatedAmount;

    /**
     * 备注
     */
    private String remark;

}
