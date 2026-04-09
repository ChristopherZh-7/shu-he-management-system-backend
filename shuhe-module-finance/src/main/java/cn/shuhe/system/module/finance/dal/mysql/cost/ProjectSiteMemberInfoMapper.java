package cn.shuhe.system.module.finance.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 项目驻场成员信息 Mapper（用于经营分析）
 * 
 * 查询通用的 project_site_member 表，支持：
 * - 安全运营驻场 (dept_type = 2)
 * - 安全服务驻场 (dept_type = 1)
 * - 数据安全驻场 (dept_type = 3)
 */
@Mapper
public interface ProjectSiteMemberInfoMapper {

    /**
     * 查询用户参与的所有驻场记录
     * 
     * 费用来源：finance_allocation 表（Level 1 部门分配金额，按 contract + dept_type 聚合）
     * 
     * @param userId 用户ID
     * @param deptType 部门类型：1-安全服务 2-安全运营 3-数据安全，传 null 查全部
     * @return 驻场参与记录列表
     */
    @Select("<script>" +
            "SELECT " +
            "  psm.id as memberId, " +
            "  psm.site_id as siteId, " +
            "  psm.project_id as projectId, " +
            "  psm.dept_type as deptType, " +
            "  psm.member_type as memberType, " +
            "  psm.start_date as memberStartDate, " +
            "  psm.end_date as memberEndDate, " +
            "  psm.status as memberStatus, " +
            "  psm.is_leader as isLeader, " +
            "  ps.name as siteName, " +
            "  p.name as projectName, " +
            "  p.customer_id as customerId, " +
            "  p.customer_name as customerName, " +
            "  p.contract_id as crmContractId, " +
            "  p.contract_no as contractNo, " +
            "  CONCAT(p.customer_name, '-', p.contract_no) as contractName, " +
            "  cc.start_time as contractStartDate, " +
            "  cc.end_time as contractEndDate, " +
            "  COALESCE(fa_sum.service_amount, 0) as onsiteFee, " +
            "  GREATEST(COALESCE(fa_sum.dept_amount, 0) - COALESCE(fa_sum.service_amount, 0), 0) as managementFee, " +
            "  COALESCE(vc.member_count, 1) as sameMemberTypeCount " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = p.contract_id AND cc.deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT fa1.contract_id, sd.dept_type, " +
            "    SUM(fa1.allocated_amount) as dept_amount, " +
            "    COALESCE(SUM(fa3.allocated_amount), 0) as service_amount " +
            "  FROM finance_allocation fa1 " +
            "  JOIN system_dept sd ON sd.id = fa1.dept_id AND sd.deleted = 0 " +
            "  LEFT JOIN finance_allocation fa2 ON fa2.parent_id = fa1.id AND fa2.allocation_level = 2 AND fa2.deleted = 0 " +
            "  LEFT JOIN finance_allocation fa3 ON fa3.parent_id = fa2.id AND fa3.allocation_level = 3 AND fa3.deleted = 0 " +
            "  WHERE fa1.allocation_level = 1 AND fa1.deleted = 0 " +
            "  GROUP BY fa1.contract_id, sd.dept_type" +
            ") fa_sum ON fa_sum.contract_id = cc.id AND fa_sum.dept_type = psm.dept_type " +
            "LEFT JOIN v_site_member_type_count vc ON vc.site_id = psm.site_id AND vc.member_type = psm.member_type " +
            "WHERE psm.user_id = #{userId} " +
            "  AND psm.deleted = 0 " +
            "  AND psm.status = 1 " +
            "<if test='deptType != null'>" +
            "  AND psm.dept_type = #{deptType} " +
            "</if>" +
            "</script>")
    List<Map<String, Object>> selectMemberParticipation(@Param("userId") Long userId,
                                                         @Param("deptType") Integer deptType);

    /**
     * 批量查询多个用户的驻场参与记录（性能优化：一次查询替代 N 次查询）
     * 
     * @param userIds 用户ID列表
     * @param deptType 部门类型：1-安全服务 2-安全运营 3-数据安全，传 null 查全部
     * @return 驻场参与记录列表（包含 userId 字段用于分组）
     */
    @Select("<script>" +
            "SELECT " +
            "  psm.user_id as userId, " +
            "  psm.id as memberId, " +
            "  psm.site_id as siteId, " +
            "  psm.project_id as projectId, " +
            "  psm.dept_type as deptType, " +
            "  psm.member_type as memberType, " +
            "  psm.start_date as memberStartDate, " +
            "  psm.end_date as memberEndDate, " +
            "  psm.status as memberStatus, " +
            "  psm.is_leader as isLeader, " +
            "  ps.name as siteName, " +
            "  p.name as projectName, " +
            "  p.customer_id as customerId, " +
            "  p.customer_name as customerName, " +
            "  p.contract_id as crmContractId, " +
            "  p.contract_no as contractNo, " +
            "  CONCAT(p.customer_name, '-', p.contract_no) as contractName, " +
            "  cc.start_time as contractStartDate, " +
            "  cc.end_time as contractEndDate, " +
            "  COALESCE(fa_sum.service_amount, 0) as onsiteFee, " +
            "  GREATEST(COALESCE(fa_sum.dept_amount, 0) - COALESCE(fa_sum.service_amount, 0), 0) as managementFee, " +
            "  COALESCE(vc.member_count, 1) as sameMemberTypeCount " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = p.contract_id AND cc.deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT fa1.contract_id, sd.dept_type, " +
            "    SUM(fa1.allocated_amount) as dept_amount, " +
            "    COALESCE(SUM(fa3.allocated_amount), 0) as service_amount " +
            "  FROM finance_allocation fa1 " +
            "  JOIN system_dept sd ON sd.id = fa1.dept_id AND sd.deleted = 0 " +
            "  LEFT JOIN finance_allocation fa2 ON fa2.parent_id = fa1.id AND fa2.allocation_level = 2 AND fa2.deleted = 0 " +
            "  LEFT JOIN finance_allocation fa3 ON fa3.parent_id = fa2.id AND fa3.allocation_level = 3 AND fa3.deleted = 0 " +
            "  WHERE fa1.allocation_level = 1 AND fa1.deleted = 0 " +
            "  GROUP BY fa1.contract_id, sd.dept_type" +
            ") fa_sum ON fa_sum.contract_id = cc.id AND fa_sum.dept_type = psm.dept_type " +
            "LEFT JOIN v_site_member_type_count vc ON vc.site_id = psm.site_id AND vc.member_type = psm.member_type " +
            "WHERE psm.user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}" +
            "</foreach>" +
            "  AND psm.deleted = 0 " +
            "  AND psm.status = 1 " +
            "<if test='deptType != null'>" +
            "  AND psm.dept_type = #{deptType} " +
            "</if>" +
            "</script>")
    List<Map<String, Object>> selectMemberParticipationBatch(@Param("userIds") List<Long> userIds, 
                                                              @Param("deptType") Integer deptType);

    /**
     * 查询指定部门类型的所有驻场成员（用于批量计算收入）
     */
    @Select("<script>" +
            "SELECT DISTINCT psm.user_id as userId " +
            "FROM project_site_member psm " +
            "WHERE psm.deleted = 0 " +
            "  AND psm.status = 1 " +
            "<if test='deptType != null'>" +
            "  AND psm.dept_type = #{deptType} " +
            "</if>" +
            "</script>")
    List<Long> selectDistinctUserIdsByDeptType(@Param("deptType") Integer deptType);

    /**
     * 查询管理类项目（project_type=2）中每个成员的收入份额。
     * 收入 = SUM(finance_allocation.allocated_amount) / 项目成员数
     */
    @Select("SELECT " +
            "  pm.user_id as userId, " +
            "  pm.project_id as projectId, " +
            "  p.name as projectName, " +
            "  p.customer_name as customerName, " +
            "  p.contract_id as contractId, " +
            "  pds.dept_type as deptType, " +
            "  COALESCE(fa_total.total_amount, 0) as totalAllocation, " +
            "  COALESCE(mc.member_count, 1) as memberCount " +
            "FROM project_member pm " +
            "JOIN project p ON p.id = pm.project_id AND p.deleted = 0 AND p.project_type = 2 " +
            "LEFT JOIN project_dept_service pds ON pds.project_id = p.id AND pds.deleted = 0 " +
            "LEFT JOIN (" +
            "  SELECT fa.contract_id, SUM(fa.allocated_amount) as total_amount " +
            "  FROM finance_allocation fa " +
            "  WHERE fa.allocation_level = 1 AND fa.deleted = 0 " +
            "  GROUP BY fa.contract_id" +
            ") fa_total ON fa_total.contract_id = p.contract_id " +
            "LEFT JOIN (" +
            "  SELECT project_id, COUNT(*) as member_count " +
            "  FROM project_member WHERE deleted = 0 " +
            "  GROUP BY project_id" +
            ") mc ON mc.project_id = p.id " +
            "WHERE pm.deleted = 0")
    List<Map<String, Object>> selectManagementProjectMemberIncome();

    /**
     * 诊断查询：获取所有驻场成员记录
     */
    @Select("SELECT " +
            "  psm.id, " +
            "  psm.site_id as siteId, " +
            "  psm.project_id as projectId, " +
            "  psm.user_id as userId, " +
            "  psm.dept_type as deptType, " +
            "  psm.member_type as memberType, " +
            "  psm.start_date as startDate, " +
            "  psm.end_date as endDate, " +
            "  psm.status as memberStatus, " +
            "  psm.is_leader as isLeader, " +
            "  ps.name as siteName, " +
            "  p.name as projectName, " +
            "  p.customer_name as customerName " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "WHERE psm.deleted = 0 " +
            "ORDER BY psm.dept_type, psm.project_id, psm.site_id, psm.user_id")
    List<Map<String, Object>> selectAllMembersForDiagnostic();

}
