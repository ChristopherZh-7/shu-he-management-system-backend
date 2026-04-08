package cn.shuhe.system.module.finance.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("finance_allocation")
@KeySequence("finance_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAllocationDO extends BaseDO {

    @TableId
    private Long id;

    private Long parentId;
    private Long contractId;
    private Long deptServiceId;
    private Long serviceItemId;
    private Integer allocationLevel;
    private String allocationType;
    private Long deptId;
    private String deptName;
    private Integer deptType;
    private String name;
    private BigDecimal allocatedAmount;
    private String remark;

}
