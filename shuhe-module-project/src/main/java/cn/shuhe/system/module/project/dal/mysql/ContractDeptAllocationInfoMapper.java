package cn.shuhe.system.module.project.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 合同部门分配信息查询 Mapper
 * 
 * 用于跨模块查询合同部门分配信息（避免循环依赖）
 * 专门为安全运营模块获取分配金额信息
 */
@Mapper
public interface ContractDeptAllocationInfoMapper {

    /**
     * 根据合同ID和部门ID查询部门分配信息
     * 
     * @param contractId 合同ID
     * @param deptId 部门ID
     * @return 包含分配信息的 Map
     */
    @Select("SELECT id as allocationId, allocated_amount as allocatedAmount, " +
            "contract_no as contractNo, customer_name as customerName " +
            "FROM contract_dept_allocation " +
            "WHERE contract_id = #{contractId} AND dept_id = #{deptId} AND deleted = 0")
    Map<String, Object> selectByContractIdAndDeptId(@Param("contractId") Long contractId, 
                                                     @Param("deptId") Long deptId);

    /**
     * 根据合同ID和部门类型查询部门分配信息
     * deptType = 2 代表安全运营服务部
     * 
     * @param contractId 合同ID
     * @param deptType 部门类型
     * @return 包含分配信息的 Map
     */
    @Select("SELECT cda.id as allocationId, cda.allocated_amount as allocatedAmount, " +
            "cda.contract_no as contractNo, cda.customer_name as customerName, " +
            "cda.dept_id as deptId, cda.dept_name as deptName " +
            "FROM contract_dept_allocation cda " +
            "INNER JOIN system_dept d ON d.id = cda.dept_id AND d.deleted = 0 " +
            "WHERE cda.contract_id = #{contractId} AND d.dept_type = #{deptType} AND cda.deleted = 0 " +
            "LIMIT 1")
    Map<String, Object> selectByContractIdAndDeptType(@Param("contractId") Long contractId, 
                                                       @Param("deptType") Integer deptType);

    /**
     * 根据合同ID查询安全运营费用分配
     * 查询管理费和驻场费
     * 
     * @param contractId 合同ID
     * @return 包含管理费和驻场费的 Map
     */
    @Select("SELECT " +
            "SUM(CASE WHEN sia.allocation_type = 'so_management' THEN sia.allocated_amount ELSE 0 END) as managementFee, " +
            "SUM(CASE WHEN sia.allocation_type = 'so_onsite' THEN sia.allocated_amount ELSE 0 END) as onsiteFee " +
            "FROM contract_dept_allocation cda " +
            "INNER JOIN system_dept d ON d.id = cda.dept_id AND d.deleted = 0 AND d.dept_type = 2 " +
            "LEFT JOIN service_item_allocation sia ON sia.contract_dept_allocation_id = cda.id AND sia.deleted = 0 " +
            "WHERE cda.contract_id = #{contractId} AND cda.deleted = 0")
    Map<String, BigDecimal> selectSecurityOperationFees(@Param("contractId") Long contractId);

}
