package cn.shuhe.system.module.system.dal.mysql.cost;

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
     * 逻辑：
     * 1. 从 project_site_member 表查询用户的驻场参与记录
     * 2. 关联 project_site 获取驻场点信息
     * 3. 关联 project 获取项目信息
     * 4. 关联 crm_contract 获取合同时间和费用分配
     * 5. 使用视图 v_contract_allocation_fees 获取费用（避免子查询）
     * 6. 使用视图 v_site_member_type_count 获取成员数（避免子查询）
     * 
     * 收入计算公式：
     * 驻场人员收入 = 驻场费 × (截至今天工作日 / 总工作日) / 该驻场点驻场人员数
     * 
     * 性能优化：使用预计算视图替代子查询，大幅提升查询速度
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
            // 使用视图获取驻场费，避免子查询
            "  COALESCE(vf.total_onsite_fee, 0) as onsiteFee, " +
            // 使用视图获取管理费
            "  COALESCE(vf.total_management_fee, 0) as managementFee, " +
            // 使用视图获取同类型成员数量
            "  COALESCE(vc.member_count, 1) as sameMemberTypeCount " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = p.contract_id AND cc.deleted = 0 " +
            // JOIN费用视图
            "LEFT JOIN v_contract_allocation_fees vf ON vf.contract_id = p.contract_id AND vf.dept_type = psm.dept_type " +
            // JOIN成员数视图
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
            "  COALESCE(vf.total_onsite_fee, 0) as onsiteFee, " +
            "  COALESCE(vf.total_management_fee, 0) as managementFee, " +
            "  COALESCE(vc.member_count, 1) as sameMemberTypeCount " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = p.contract_id AND cc.deleted = 0 " +
            "LEFT JOIN v_contract_allocation_fees vf ON vf.contract_id = p.contract_id AND vf.dept_type = psm.dept_type " +
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
