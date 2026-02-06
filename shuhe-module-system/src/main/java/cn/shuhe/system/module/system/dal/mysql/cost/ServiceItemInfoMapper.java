package cn.shuhe.system.module.system.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务项信息 Mapper
 * 用于成本汇总查询，直接查询 project_info 表
 */
@Mapper
public interface ServiceItemInfoMapper {

    /**
     * 根据服务项ID查询服务项信息
     */
    @Select("SELECT id, code, name, dept_type as deptType, service_type as serviceType, " +
            "customer_id as customerId, customer_name as customerName, " +
            "contract_id as contractId, contract_no as contractNo, " +
            "dept_id as deptId, frequency_type as frequencyType, " +
            "max_count as maxCount, used_count as usedCount, status " +
            "FROM project_info " +
            "WHERE id = #{serviceItemId} AND deleted = 0")
    Map<String, Object> selectServiceItemInfo(@Param("serviceItemId") Long serviceItemId);

    /**
     * 根据合同ID查询该合同下的所有服务项
     */
    @Select("SELECT id, code, name, dept_type as deptType, service_type as serviceType, " +
            "customer_id as customerId, customer_name as customerName, " +
            "contract_id as contractId, contract_no as contractNo, " +
            "dept_id as deptId, max_count as maxCount, used_count as usedCount, status " +
            "FROM project_info " +
            "WHERE contract_id = #{contractId} AND deleted = 0")
    List<Map<String, Object>> selectByContractId(@Param("contractId") Long contractId);

    /**
     * 根据部门ID查询该部门的所有服务项
     */
    @Select("SELECT id, code, name, dept_type as deptType, service_type as serviceType, " +
            "customer_id as customerId, customer_name as customerName, " +
            "contract_id as contractId, contract_no as contractNo, " +
            "dept_id as deptId, max_count as maxCount, used_count as usedCount, status " +
            "FROM project_info " +
            "WHERE dept_id = #{deptId} AND deleted = 0")
    List<Map<String, Object>> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询合同下已领取服务的部门列表
     * 
     * 两种来源：
     * 1. 部门服务单（project_dept_service）：已领取且状态为进行中/已完成的部门
     *    - 优先使用 actual_dept_id（根据负责人所在部门确定的实际执行部门）
     *    - 如果没有则使用 dept_id（合同分派时指定的部门）
     * 2. 服务发起（project_service_launch）：已通过审批的服务发起中的执行部门
     */
    @Select("SELECT DISTINCT deptId, deptName FROM (" +
            // 来源1：从部门服务单获取已领取的部门（优先使用实际执行部门）
            "  SELECT COALESCE(pds.actual_dept_id, pds.dept_id) as deptId, " +
            "         COALESCE(pds.actual_dept_name, pds.dept_name) as deptName " +
            "  FROM project_dept_service pds " +
            "  WHERE pds.contract_id = #{contractId} " +
            "    AND pds.deleted = 0 " +
            "    AND pds.claimed = 1 " +  // 已领取
            "    AND pds.status IN (2, 4) " +  // 进行中或已完成
            "    AND COALESCE(pds.actual_dept_id, pds.dept_id) IS NOT NULL " +
            "  UNION " +
            // 来源2：从服务发起获取执行部门
            "  SELECT COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) as deptId, " +
            "         d.name as deptName " +
            "  FROM project_service_launch psl " +
            "  JOIN project_info pi ON pi.id = psl.service_item_id " +
            "  LEFT JOIN system_dept d ON COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) = d.id " +
            "  WHERE pi.contract_id = #{contractId} " +
            "    AND psl.deleted = 0 " +
            "    AND psl.status = 1 " +  // 已通过
            "    AND COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) IS NOT NULL " +
            ") t WHERE deptId IS NOT NULL")
    List<Map<String, Object>> selectDeptsByContractId(@Param("contractId") Long contractId);

    /**
     * 统计服务项已完成的轮次数（截至指定日期）
     * 轮次状态：0待执行 1执行中 2已完成 3已取消
     */
    @Select("SELECT COUNT(*) FROM project_round " +
            "WHERE service_item_id = #{serviceItemId} AND status = 2 AND deleted = 0 " +
            "AND actual_end_time <= #{cutoffDate}")
    Integer countCompletedRounds(@Param("serviceItemId") Long serviceItemId, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 统计服务项已完成的轮次数（不限时间）
     */
    @Select("SELECT COUNT(*) FROM project_round " +
            "WHERE service_item_id = #{serviceItemId} AND status = 2 AND deleted = 0")
    Integer countCompletedRoundsAll(@Param("serviceItemId") Long serviceItemId);

    /**
     * 根据合同ID和执行部门ID查询服务项列表（基于服务发起记录）
     * 只查询该部门实际执行过的服务项
     */
    @Select("SELECT DISTINCT " +
            "  pi.id, pi.code, pi.name, pi.dept_type as deptType, pi.service_type as serviceType, " +
            "  pi.customer_id as customerId, pi.customer_name as customerName, " +
            "  pi.contract_id as contractId, pi.contract_no as contractNo, " +
            "  pi.dept_id as deptId, pi.frequency_type as frequencyType, " +
            "  pi.max_count as maxCount, pi.used_count as usedCount, pi.status " +
            "FROM project_info pi " +
            "JOIN project_service_launch psl ON psl.service_item_id = pi.id " +
            "WHERE pi.contract_id = #{contractId} " +
            "  AND COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) = #{deptId} " +
            "  AND psl.status = 1 " +  // 只统计已通过的服务发起
            "  AND pi.deleted = 0 " +
            "  AND psl.deleted = 0")
    List<Map<String, Object>> selectServiceItemsByContractAndDept(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    /**
     * 根据合同ID和费用类型查询可分配的服务项列表
     * 
     * 费用类型与服务项类型对应关系：
     * - ss_onsite（安全服务驻场费）→ deptType=1 AND serviceMode=1
     * - ss_second_line（安全服务二线费）→ deptType=1 AND (serviceMode=2 OR serviceMode IS NULL)
     * - so_onsite（安全运营驻场费）→ deptType=2 AND serviceMemberType=1
     * - so_management（安全运营管理费）→ deptType=2 AND (serviceMemberType=2 OR serviceMemberType IS NULL)
     * 
     * @param contractId 合同ID
     * @param deptType 部门类型（1-安全服务，2-安全运营）
     * @param serviceMode 服务模式（安全服务专用：1-驻场，2-二线）
     * @param serviceMemberType 服务归属人员类型（安全运营专用：1-驻场人员，2-管理人员）
     */
    @Select("<script>" +
            "SELECT id, code, name, dept_type as deptType, service_type as serviceType, " +
            "  service_mode as serviceMode, service_member_type as serviceMemberType, " +
            "  customer_id as customerId, customer_name as customerName, " +
            "  contract_id as contractId, contract_no as contractNo, " +
            "  dept_id as deptId, frequency_type as frequencyType, " +
            "  max_count as maxCount, used_count as usedCount, status " +
            "FROM project_info " +
            "WHERE contract_id = #{contractId} AND deleted = 0 " +
            "  AND dept_type = #{deptType} " +
            "<if test='serviceMode != null and serviceMode == 1'>" +
            "  AND service_mode = 1 " +
            "</if>" +
            "<if test='serviceMode != null and serviceMode == 2'>" +
            "  AND (service_mode = 2 OR service_mode IS NULL) " +
            "</if>" +
            "<if test='serviceMemberType != null and serviceMemberType == 1'>" +
            "  AND service_member_type = 1 " +
            "</if>" +
            "<if test='serviceMemberType != null and serviceMemberType == 2'>" +
            "  AND (service_member_type = 2 OR service_member_type IS NULL) " +
            "</if>" +
            "ORDER BY id ASC" +
            "</script>")
    List<Map<String, Object>> selectServiceItemsByContractAndType(
            @Param("contractId") Long contractId, 
            @Param("deptType") Integer deptType,
            @Param("serviceMode") Integer serviceMode,
            @Param("serviceMemberType") Integer serviceMemberType);

    /**
     * 查询用户作为执行人完成的轮次（带服务项分配金额）
     * 用于计算安全服务/数据安全员工的收入
     * 注意：executor_ids 可能是 JSON 数组或逗号分隔的字符串
     * 
     * 时间判断逻辑：
     * - 优先使用 actual_end_time（实际结束时间）
     * - 如果 actual_end_time 为 NULL，则使用 update_time（更新时间）作为替代
     * 
     * 分配金额获取逻辑（修复版）：
     * - 通过轮次的 service_launch_id 关联服务发起，获取执行部门
     * - 通过执行部门关联合同部门分配，获取正确的分配金额
     * - 如果轮次没有 service_launch_id，则取该服务项所有分配金额的总和
     */
    @Select("SELECT " +
            "  pr.id as roundId, " +
            "  pr.service_item_id as serviceItemId, " +
            "  pr.round_no as roundNumber, " +
            "  pr.executor_ids as executorIds, " +
            "  COALESCE(pr.actual_end_time, pr.update_time) as actualEndTime, " +
            "  pi.name as serviceItemName, " +
            "  pi.customer_name as customerName, " +
            "  pi.max_count as maxCount, " +
            "  pi.frequency_type as frequencyType, " +
            // 优先通过服务发起关联到执行部门，再关联到正确的分配记录
            "  COALESCE(" +
            "    (SELECT sia2.allocated_amount FROM service_item_allocation sia2 " +
            "     JOIN contract_dept_allocation cda ON cda.id = sia2.contract_dept_allocation_id AND cda.deleted = 0 " +
            "     JOIN project_service_launch psl ON COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) = cda.dept_id " +
            "     WHERE sia2.service_item_id = pi.id AND sia2.deleted = 0 " +
            "       AND psl.id = pr.service_launch_id AND psl.deleted = 0 " +
            "       AND cda.contract_id = pi.contract_id " +
            "     LIMIT 1), " +
            // 兜底：取该服务项所有分配金额的总和
            "    (SELECT SUM(sia3.allocated_amount) FROM service_item_allocation sia3 " +
            "     WHERE sia3.service_item_id = pi.id AND sia3.deleted = 0), " +
            "    0" +
            "  ) as allocatedAmount " +
            "FROM project_round pr " +
            "JOIN project_info pi ON pi.id = pr.service_item_id " +
            "WHERE pr.deleted = 0 " +
            "  AND pr.status = 2 " +
            "  AND YEAR(COALESCE(pr.actual_end_time, pr.update_time)) = #{year} " +
            "  AND COALESCE(pr.actual_end_time, pr.update_time) <= #{cutoffDate} " +
            "  AND (pr.executor_ids LIKE CONCAT('%', #{userId}, '%') " +
            "       OR pr.executor_ids LIKE CONCAT('%\"', #{userId}, '\"%') " +
            "       OR pr.executor_ids LIKE CONCAT('%[', #{userId}, ']%') " +
            "       OR pr.executor_ids LIKE CONCAT('%[', #{userId}, ',%') " +
            "       OR pr.executor_ids LIKE CONCAT('%,', #{userId}, ']%') " +
            "       OR pr.executor_ids LIKE CONCAT('%,', #{userId}, ',%'))")
    List<Map<String, Object>> selectCompletedRoundsByExecutor(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 批量查询已完成的服务轮次（性能优化：一次查询所有，Java侧按executorIds过滤）
     * 
     * 返回所有符合时间条件的已完成轮次，包含executorIds字段用于Java侧过滤
     */
    @Select("SELECT " +
            "  pr.id as roundId, " +
            "  pr.service_item_id as serviceItemId, " +
            "  pr.round_no as roundNumber, " +
            "  pr.executor_ids as executorIds, " +
            "  COALESCE(pr.actual_end_time, pr.update_time) as actualEndTime, " +
            "  pi.name as serviceItemName, " +
            "  pi.customer_name as customerName, " +
            "  pi.max_count as maxCount, " +
            "  pi.frequency_type as frequencyType, " +
            "  COALESCE(" +
            "    (SELECT sia2.allocated_amount FROM service_item_allocation sia2 " +
            "     JOIN contract_dept_allocation cda ON cda.id = sia2.contract_dept_allocation_id AND cda.deleted = 0 " +
            "     JOIN project_service_launch psl ON COALESCE(psl.actual_execute_dept_id, psl.execute_dept_id) = cda.dept_id " +
            "     WHERE sia2.service_item_id = pi.id AND sia2.deleted = 0 " +
            "       AND psl.id = pr.service_launch_id AND psl.deleted = 0 " +
            "       AND cda.contract_id = pi.contract_id " +
            "     LIMIT 1), " +
            "    (SELECT SUM(sia3.allocated_amount) FROM service_item_allocation sia3 " +
            "     WHERE sia3.service_item_id = pi.id AND sia3.deleted = 0), " +
            "    0" +
            "  ) as allocatedAmount " +
            "FROM project_round pr " +
            "JOIN project_info pi ON pi.id = pr.service_item_id " +
            "WHERE pr.deleted = 0 " +
            "  AND pr.status = 2 " +
            "  AND YEAR(COALESCE(pr.actual_end_time, pr.update_time)) = #{year} " +
            "  AND COALESCE(pr.actual_end_time, pr.update_time) <= #{cutoffDate} " +
            "  AND pr.executor_ids IS NOT NULL " +
            "  AND pr.executor_ids <> '' " +
            "  AND pr.executor_ids <> '[]'")
    List<Map<String, Object>> selectCompletedRoundsByExecutorBatch(
            @Param("year") int year,
            @Param("cutoffDate") LocalDateTime cutoffDate);

}
