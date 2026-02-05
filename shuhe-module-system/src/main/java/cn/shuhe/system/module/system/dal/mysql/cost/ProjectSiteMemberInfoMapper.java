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
     * 5. 从 service_item_allocation 获取驻场费分配金额
     * 
     * 收入计算公式：
     * 驻场人员收入 = 驻场费 × (截至今天工作日 / 总工作日) / 该驻场点驻场人员数
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
            // 使用 crm_contract 的开始和结束时间
            "  cc.start_time as contractStartDate, " +
            "  cc.end_time as contractEndDate, " +
            // 从 contract_dept_allocation 和 service_item_allocation 获取驻场费
            // 根据 dept_type 使用不同的 allocation_type
            "  COALESCE((" +
            "    SELECT SUM(sia.allocated_amount) FROM service_item_allocation sia " +
            "    JOIN contract_dept_allocation cda ON sia.contract_dept_allocation_id = cda.id AND cda.deleted = 0 " +
            "    WHERE cda.contract_id = p.contract_id " +
            "    AND cda.dept_type = psm.dept_type " +
            "    AND sia.allocation_type = CASE " +
            "      WHEN psm.dept_type = 2 THEN 'so_onsite' " +
            "      WHEN psm.dept_type = 1 THEN 'ss_onsite' " +
            "      WHEN psm.dept_type = 3 THEN 'ds_onsite' " +
            "      ELSE 'service_onsite' " +
            "    END " +
            "    AND sia.deleted = 0" +
            "  ), 0) as onsiteFee, " +
            // 获取管理费（仅安全运营有）
            "  COALESCE((" +
            "    SELECT SUM(sia.allocated_amount) FROM service_item_allocation sia " +
            "    JOIN contract_dept_allocation cda ON sia.contract_dept_allocation_id = cda.id AND cda.deleted = 0 " +
            "    WHERE cda.contract_id = p.contract_id " +
            "    AND cda.dept_type = psm.dept_type " +
            "    AND sia.allocation_type = 'so_management' " +
            "    AND sia.deleted = 0" +
            "  ), 0) as managementFee, " +
            // 统计该驻场点下同类型成员数量（在岗状态）
            "  (SELECT COUNT(*) FROM project_site_member psm2 " +
            "   WHERE psm2.deleted = 0 AND psm2.status = 1 " +
            "   AND psm2.site_id = psm.site_id " +
            "   AND psm2.member_type = psm.member_type) as sameMemberTypeCount " +
            "FROM project_site_member psm " +
            "LEFT JOIN project_site ps ON ps.id = psm.site_id AND ps.deleted = 0 " +
            "LEFT JOIN project p ON p.id = psm.project_id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract cc ON cc.id = p.contract_id AND cc.deleted = 0 " +
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
