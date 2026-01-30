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
     * 查询合同下已领取服务项的部门列表
     */
    @Select("SELECT DISTINCT pi.dept_id as deptId, d.name as deptName " +
            "FROM project_info pi " +
            "LEFT JOIN system_dept d ON pi.dept_id = d.id " +
            "WHERE pi.contract_id = #{contractId} AND pi.deleted = 0 AND pi.dept_id IS NOT NULL")
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
     * 根据合同ID和部门ID查询服务项列表
     */
    @Select("SELECT id, code, name, dept_type as deptType, service_type as serviceType, " +
            "customer_id as customerId, customer_name as customerName, " +
            "contract_id as contractId, contract_no as contractNo, " +
            "dept_id as deptId, frequency_type as frequencyType, " +
            "max_count as maxCount, used_count as usedCount, status " +
            "FROM project_info " +
            "WHERE contract_id = #{contractId} AND dept_id = #{deptId} AND deleted = 0")
    List<Map<String, Object>> selectServiceItemsByContractAndDept(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    /**
     * 查询用户作为执行人完成的轮次（带服务项分配金额）
     * 用于计算安全服务/数据安全员工的收入
     * 注意：executor_ids 可能是 JSON 数组或逗号分隔的字符串
     * 
     * 时间判断逻辑：
     * - 优先使用 actual_end_time（实际结束时间）
     * - 如果 actual_end_time 为 NULL，则使用 update_time（更新时间）作为替代
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
            "  COALESCE(sia.allocated_amount, 0) as allocatedAmount " +
            "FROM project_round pr " +
            "JOIN project_info pi ON pi.id = pr.service_item_id " +
            "LEFT JOIN service_item_allocation sia ON sia.service_item_id = pi.id AND sia.deleted = 0 " +
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

}
