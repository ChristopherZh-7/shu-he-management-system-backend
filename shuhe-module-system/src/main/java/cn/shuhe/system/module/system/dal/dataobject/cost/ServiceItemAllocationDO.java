package cn.shuhe.system.module.system.dal.dataobject.cost;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 服务项金额分配 DO
 * 
 * 记录部门金额分配到各服务项的情况（第二级分配）
 * 也支持安全运营合同的管理费/驻场费分配
 */
@TableName("service_item_allocation")
@KeySequence("service_item_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemAllocationDO extends BaseDO {

    // ========== 分配类型常量 ==========
    
    /**
     * 分配类型：服务项分配（默认）
     */
    public static final String ALLOCATION_TYPE_SERVICE_ITEM = "service_item";
    
    /**
     * 分配类型：安全运营管理费分配
     */
    public static final String ALLOCATION_TYPE_SO_MANAGEMENT = "so_management";
    
    /**
     * 分配类型：安全运营驻场费分配
     */
    public static final String ALLOCATION_TYPE_SO_ONSITE = "so_onsite";

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 合同部门分配ID
     * 
     * 关联 {@link ContractDeptAllocationDO#getId()}
     */
    private Long contractDeptAllocationId;

    /**
     * 分配类型
     * 
     * service_item - 服务项分配（默认）
     * so_management - 安全运营管理费分配
     * so_onsite - 安全运营驻场费分配
     */
    private String allocationType;

    /**
     * 服务项ID（allocationType为service_item时有效）
     */
    private Long serviceItemId;

    /**
     * 服务项名称（冗余，方便查询展示）
     * 对于安全运营分配，存储"管理费"或"驻场费"
     */
    private String serviceItemName;

    /**
     * 分配金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 备注
     */
    private String remark;

}
