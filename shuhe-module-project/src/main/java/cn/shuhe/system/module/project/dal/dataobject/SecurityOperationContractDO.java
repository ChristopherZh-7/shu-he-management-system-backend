package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 安全运营合同 DO
 * 
 * 记录安全运营部承接的合同信息
 * 安全运营 = 管理费 + 驻场费
 * 服务项来自合同，由驻场人员执行
 */
@TableName("security_operation_contract")
@KeySequence("security_operation_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityOperationContractDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    // ========== 合同关联 ==========

    /**
     * 合同ID（关联crm_contract）
     */
    private Long contractId;

    /**
     * 合同编号（冗余）
     */
    private String contractNo;

    /**
     * 合同部门分配ID
     */
    private Long contractDeptAllocationId;

    // ========== 客户信息 ==========

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称（冗余）
     */
    private String customerName;

    // ========== 驻场信息 ==========

    /**
     * 项目名称
     */
    private String name;

    /**
     * 驻场地点
     */
    private String onsiteLocation;

    /**
     * 详细地址
     */
    private String onsiteAddress;

    /**
     * 驻场开始日期
     */
    private LocalDate onsiteStartDate;

    /**
     * 驻场结束日期
     */
    private LocalDate onsiteEndDate;

    // ========== 费用（两大块） ==========

    /**
     * 管理费（元）
     */
    private BigDecimal managementFee;

    /**
     * 驻场费（元）
     */
    private BigDecimal onsiteFee;

    // ========== 人员统计 ==========

    /**
     * 管理人员数量
     */
    private Integer managementCount;

    /**
     * 驻场人员数量
     */
    private Integer onsiteCount;

    // ========== 状态 ==========

    /**
     * 状态：0-待启动 1-进行中 2-已结束 3-已终止
     */
    private Integer status;

}
