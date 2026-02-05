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
     * 分配类型：安全服务驻场费分配
     */
    public static final String ALLOCATION_TYPE_SS_ONSITE = "ss_onsite";

    /**
     * 分配类型：安全服务二线费分配
     */
    public static final String ALLOCATION_TYPE_SS_SECOND_LINE = "ss_second_line";

    /**
     * 分配类型：数据安全驻场费分配
     */
    public static final String ALLOCATION_TYPE_DS_ONSITE = "ds_onsite";

    /**
     * 分配类型：数据安全二线费分配
     */
    public static final String ALLOCATION_TYPE_DS_SECOND_LINE = "ds_second_line";

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
     * 父级分配ID（用于层级分配）
     * 
     * 当具体服务项从费用类型分配中再分配时，指向父级分配记录的ID
     * 例如：从"二线服务费"分配到具体的二线服务项时，此字段指向"二线服务费"的分配记录
     * 为 null 表示这是一级分配（费用类型分配）
     */
    private Long parentAllocationId;

    /**
     * 分配类型
     * 
     * service_item - 服务项分配（从费用类型分配到具体服务项）
     * so_management - 安全运营管理费分配（费用类型级别）
     * so_onsite - 安全运营驻场费分配（费用类型级别）
     * ss_onsite - 安全服务驻场费分配（费用类型级别）
     * ss_second_line - 安全服务二线费分配（费用类型级别）
     * ds_onsite - 数据安全驻场费分配（费用类型级别）
     * ds_second_line - 数据安全二线费分配（费用类型级别）
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
