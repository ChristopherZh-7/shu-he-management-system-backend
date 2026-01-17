package cn.shuhe.system.module.crm.dal.dataobject.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 合同分派部门信息
 * 用于记录每个分派部门的领取状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAssignDeptInfo {

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 是否已领取
     */
    private Boolean claimed;

    /**
     * 领取人用户ID
     */
    private Long claimUserId;

    /**
     * 领取人名称
     */
    private String claimUserName;

    /**
     * 领取时间
     */
    private LocalDateTime claimTime;

}
