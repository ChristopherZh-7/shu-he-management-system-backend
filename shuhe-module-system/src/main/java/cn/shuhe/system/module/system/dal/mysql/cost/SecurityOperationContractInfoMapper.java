package cn.shuhe.system.module.system.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 安全运营合同信息 Mapper
 * 用于成本汇总查询，直接查询 security_operation_contract 表
 */
@Mapper
public interface SecurityOperationContractInfoMapper {

    /**
     * 根据合同ID查询安全运营合同信息
     */
    @Select("SELECT id, contract_id as contractId, contract_no as contractNo, " +
            "customer_name as customerName, name, " +
            "onsite_start_date as onsiteStartDate, onsite_end_date as onsiteEndDate, " +
            "management_fee as managementFee, onsite_fee as onsiteFee, status " +
            "FROM security_operation_contract " +
            "WHERE contract_id = #{contractId} AND deleted = 0")
    Map<String, Object> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 查询所有进行中的安全运营合同
     * 状态：0-待启动 1-进行中 2-已结束 3-已终止
     * 只查询进行中(1)和待启动(0)的合同
     */
    @Select("SELECT id, contract_id as contractId, contract_no as contractNo, " +
            "customer_name as customerName, name, " +
            "onsite_start_date as onsiteStartDate, onsite_end_date as onsiteEndDate, " +
            "management_fee as managementFee, onsite_fee as onsiteFee, status " +
            "FROM security_operation_contract " +
            "WHERE deleted = 0 AND status IN (0, 1)")
    List<Map<String, Object>> selectActiveContracts();

    /**
     * 查询截止日期前有效的安全运营合同
     * 合同开始日期 <= 截止日期
     */
    @Select("SELECT id, contract_id as contractId, contract_no as contractNo, " +
            "customer_name as customerName, name, " +
            "onsite_start_date as onsiteStartDate, onsite_end_date as onsiteEndDate, " +
            "management_fee as managementFee, onsite_fee as onsiteFee, status " +
            "FROM security_operation_contract " +
            "WHERE deleted = 0 AND onsite_start_date <= #{cutoffDate}")
    List<Map<String, Object>> selectContractsByCutoffDate(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 根据合同部门分配ID查询安全运营合同
     */
    @Select("SELECT id, contract_id as contractId, contract_no as contractNo, " +
            "customer_name as customerName, name, " +
            "onsite_start_date as onsiteStartDate, onsite_end_date as onsiteEndDate, " +
            "management_fee as managementFee, onsite_fee as onsiteFee, status, " +
            "contract_dept_allocation_id as contractDeptAllocationId " +
            "FROM security_operation_contract " +
            "WHERE contract_dept_allocation_id = #{allocationId} AND deleted = 0")
    Map<String, Object> selectByAllocationId(@Param("allocationId") Long allocationId);

    /**
     * 查询用户参与的所有安全运营合同
     * 
     * 逻辑（已修正 v6 - 使用视图优化性能，避免子查询）：
     * 1. 从 security_operation_member 表查询用户的实际参与记录
     * 2. 根据 member_type 确定是管理人员(1)还是驻场人员(2)
     * 3. 管理人员获得管理费份额，驻场人员获得驻场费份额
     * 4. 费用从视图 v_so_contract_fees 读取（预计算，避免子查询）
     * 5. 成员数从视图 v_so_contract_member_type_count 读取（预计算，避免子查询）
     * 
     * 关联路径：
     * security_operation_member.so_contract_id 
     *   → security_operation_contract.contract_dept_allocation_id
     *   → v_so_contract_fees（读取 management_fee / onsite_fee）
     *   → crm_contract（获取合同开始/结束时间）
     * 
     * 收入计算公式：
     * - 管理人员：管理费 × (截至今天工作日 / 总工作日) / 该合同管理人员数
     * - 驻场人员：驻场费 × (截至今天工作日 / 总工作日) / 该合同驻场人员数
     * 
     * 性能优化：使用预计算视图替代子查询，大幅提升查询速度
     */
    @Select("SELECT " +
            "  som.id as memberId, " +
            "  som.so_contract_id as soContractId, " +
            "  som.site_id as siteId, " +
            "  som.member_type as memberType, " +
            "  som.start_date as memberStartDate, " +
            "  som.end_date as memberEndDate, " +
            "  som.status as memberStatus, " +
            "  som.is_leader as isLeader, " +
            "  sos.name as siteName, " +
            "  soc.id as securityOperationContractId, " +
            "  soc.name as projectName, " +
            "  soc.contract_id as crmContractId, " +
            "  soc.contract_dept_allocation_id as contractDeptAllocationId, " +
            "  soc.customer_name as customerName, " +
            "  soc.contract_no as contractNo, " +
            "  CONCAT(soc.customer_name, '-', soc.contract_no) as contractName, " +
            "  cc.start_time as contractStartDate, " +
            "  cc.end_time as contractEndDate, " +
            // 使用视图获取费用，避免子查询
            "  COALESCE(vf.management_fee, 0) as managementFee, " +
            "  COALESCE(vf.onsite_fee, 0) as onsiteFee, " +
            // 使用视图获取成员数量，避免子查询
            "  COALESCE(vc.member_count, 1) as sameMemberTypeCount " +
            "FROM security_operation_member som " +
            "LEFT JOIN security_operation_site sos ON sos.id = som.site_id AND sos.deleted = 0 " +
            "LEFT JOIN security_operation_contract soc ON soc.id = som.so_contract_id AND soc.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = soc.contract_id AND cc.deleted = 0 " +
            // JOIN费用视图
            "LEFT JOIN v_so_contract_fees vf ON vf.so_contract_id = soc.id " +
            // JOIN成员数视图
            "LEFT JOIN v_so_contract_member_type_count vc ON vc.so_contract_id = som.so_contract_id AND vc.member_type = som.member_type " +
            "WHERE som.user_id = #{userId} " +
            "  AND som.deleted = 0 " +
            "  AND som.status = 1")  // 只查询在岗状态的成员
    List<Map<String, Object>> selectMemberParticipation(@Param("userId") Long userId);

    /**
     * 根据合同部门分配ID查询安全运营收入计算所需信息
     * 
     * 返回：
     * - 合同开始/结束时间（来自 crm_contract）
     * - 管理费分配金额（来自 service_item_allocation，allocation_type='so_management'）
     * - 驻场费分配金额（来自 service_item_allocation，allocation_type='so_onsite'）
     */
    @Select("SELECT " +
            "  cda.id as contractDeptAllocationId, " +
            "  cda.contract_id as contractId, " +
            "  cda.allocated_amount as totalAllocatedAmount, " +
            "  cc.start_time as contractStartDate, " +
            "  cc.end_time as contractEndDate, " +
            "  (SELECT COALESCE(SUM(sia.allocated_amount), 0) FROM service_item_allocation sia " +
            "   WHERE sia.contract_dept_allocation_id = cda.id AND sia.allocation_type = 'so_management' AND sia.deleted = 0) as managementFee, " +
            "  (SELECT COALESCE(SUM(sia.allocated_amount), 0) FROM service_item_allocation sia " +
            "   WHERE sia.contract_dept_allocation_id = cda.id AND sia.allocation_type = 'so_onsite' AND sia.deleted = 0) as onsiteFee " +
            "FROM contract_dept_allocation cda " +
            "LEFT JOIN crm_contract cc ON cc.id = cda.contract_id AND cc.deleted = 0 " +
            "WHERE cda.id = #{allocationId} AND cda.deleted = 0")
    Map<String, Object> selectOperationIncomeInfo(@Param("allocationId") Long allocationId);

    /**
     * 诊断查询：获取所有安全运营成员记录
     * 用于排查经营分析中安全运营收入问题
     * 
     * 从 service_item_allocation 表读取管理费和驻场费（与经营分析保持一致）
     */
    @Select("SELECT " +
            "  som.id, " +
            "  som.so_contract_id as soContractId, " +
            "  som.site_id as siteId, " +
            "  som.user_id as userId, " +
            "  som.member_type as memberType, " +
            "  som.start_date as startDate, " +
            "  som.end_date as endDate, " +
            "  som.status as memberStatus, " +
            "  som.is_leader as isLeader, " +
            "  sos.name as siteName, " +
            "  soc.name as contractName, " +
            "  soc.contract_id as crmContractId, " +
            "  soc.contract_dept_allocation_id as contractDeptAllocationId, " +
            "  soc.contract_no as contractNo, " +
            "  soc.customer_name as customerName, " +
            // 从 service_item_allocation 表读取费用（主数据源）
            "  COALESCE((SELECT SUM(sia.allocated_amount) FROM service_item_allocation sia " +
            "   WHERE sia.contract_dept_allocation_id = soc.contract_dept_allocation_id " +
            "   AND sia.allocation_type = 'so_management' AND sia.deleted = 0), 0) as managementFee, " +
            "  COALESCE((SELECT SUM(sia.allocated_amount) FROM service_item_allocation sia " +
            "   WHERE sia.contract_dept_allocation_id = soc.contract_dept_allocation_id " +
            "   AND sia.allocation_type = 'so_onsite' AND sia.deleted = 0), 0) as onsiteFee " +
            "FROM security_operation_member som " +
            "LEFT JOIN security_operation_site sos ON sos.id = som.site_id AND sos.deleted = 0 " +
            "LEFT JOIN security_operation_contract soc ON soc.id = som.so_contract_id AND soc.deleted = 0 " +
            "WHERE som.deleted = 0 " +
            "ORDER BY som.so_contract_id, som.site_id, som.user_id")
    List<Map<String, Object>> selectAllMembersForDiagnostic();

    /**
     * 诊断查询：获取指定合同的所有成员
     */
    @Select("SELECT " +
            "  som.id, " +
            "  som.so_contract_id as soContractId, " +
            "  som.user_id as userId, " +
            "  som.member_type as memberType, " +
            "  som.start_date as startDate, " +
            "  som.end_date as endDate, " +
            "  som.status as memberStatus, " +
            "  su.nickname as userName " +
            "FROM security_operation_member som " +
            "LEFT JOIN system_users su ON su.id = som.user_id AND su.deleted = 0 " +
            "WHERE som.so_contract_id = #{soContractId} AND som.deleted = 0 " +
            "ORDER BY som.member_type, som.user_id")
    List<Map<String, Object>> selectMembersByContractId(@Param("soContractId") Long soContractId);

    // ========== 费用同步更新方法 ==========

    /**
     * 根据合同部门分配ID更新管理费
     * 用于保持 service_item_allocation 和 security_operation_contract 数据一致
     */
    @Update("UPDATE security_operation_contract " +
            "SET management_fee = #{managementFee}, update_time = NOW() " +
            "WHERE contract_dept_allocation_id = #{allocationId} AND deleted = 0")
    int updateManagementFeeByAllocationId(@Param("allocationId") Long allocationId, 
                                          @Param("managementFee") BigDecimal managementFee);

    /**
     * 根据合同部门分配ID更新驻场费
     * 用于保持 service_item_allocation 和 security_operation_contract 数据一致
     */
    @Update("UPDATE security_operation_contract " +
            "SET onsite_fee = #{onsiteFee}, update_time = NOW() " +
            "WHERE contract_dept_allocation_id = #{allocationId} AND deleted = 0")
    int updateOnsiteFeeByAllocationId(@Param("allocationId") Long allocationId, 
                                      @Param("onsiteFee") BigDecimal onsiteFee);

    /**
     * 查询指定合同部门分配下的安全运营费用汇总
     * 从 service_item_allocation 表获取最新分配金额
     */
    @Select("SELECT " +
            "  COALESCE(SUM(CASE WHEN allocation_type = 'so_management' THEN allocated_amount ELSE 0 END), 0) as managementFee, " +
            "  COALESCE(SUM(CASE WHEN allocation_type = 'so_onsite' THEN allocated_amount ELSE 0 END), 0) as onsiteFee " +
            "FROM service_item_allocation " +
            "WHERE contract_dept_allocation_id = #{allocationId} AND deleted = 0")
    Map<String, BigDecimal> selectSecurityOperationFeesByAllocationId(@Param("allocationId") Long allocationId);

}
