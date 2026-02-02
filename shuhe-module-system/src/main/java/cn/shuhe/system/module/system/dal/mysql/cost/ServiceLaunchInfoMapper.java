package cn.shuhe.system.module.system.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 服务发起信息查询 Mapper（跨模块查询）
 * 用于成本模块查询统一服务发起信息
 */
@Mapper
public interface ServiceLaunchInfoMapper {

    /**
     * 查询服务发起信息
     * 通过JOIN获取关联表的名称信息
     * 注意：executeDeptId 优先使用 actual_execute_dept_id（审批时选择的实际执行部门），
     *       如果为空则使用 execute_dept_id（发起时选择的执行部门）
     */
    @Select("SELECT sl.id, sl.contract_id AS contractId, sl.project_id AS projectId, " +
            "sl.service_item_id AS serviceItemId, si.name AS serviceItemName, " +
            "si.service_type AS serviceType, si.dept_type AS deptType, " +
            "sl.service_item_dept_id AS serviceItemDeptId, d1.name AS serviceItemDeptName, " +
            "COALESCE(sl.actual_execute_dept_id, sl.execute_dept_id) AS executeDeptId, d2.name AS executeDeptName, " +
            "sl.request_user_id AS requestUserId, u1.nickname AS requestUserName, " +
            "sl.request_dept_id AS requestDeptId, d3.name AS requestDeptName, " +
            "sl.is_outside AS isOutside, sl.is_cross_dept AS isCrossDept, " +
            "sl.destination, sl.reason, " +
            "sl.plan_start_time AS planStartTime, sl.plan_end_time AS planEndTime, " +
            "sl.status, p.customer_name AS customerName, " +
            "c.no AS contractNo, c.name AS contractName " +
            "FROM project_service_launch sl " +
            "LEFT JOIN crm_contract c ON sl.contract_id = c.id AND c.deleted = 0 " +
            "LEFT JOIN project_info si ON sl.service_item_id = si.id AND si.deleted = 0 " +
            "LEFT JOIN project p ON sl.project_id = p.id AND p.deleted = 0 " +
            "LEFT JOIN system_dept d1 ON sl.service_item_dept_id = d1.id AND d1.deleted = 0 " +
            "LEFT JOIN system_dept d2 ON COALESCE(sl.actual_execute_dept_id, sl.execute_dept_id) = d2.id AND d2.deleted = 0 " +
            "LEFT JOIN system_dept d3 ON sl.request_dept_id = d3.id AND d3.deleted = 0 " +
            "LEFT JOIN system_users u1 ON sl.request_user_id = u1.id AND u1.deleted = 0 " +
            "WHERE sl.id = #{launchId} AND sl.deleted = 0")
    Map<String, Object> selectServiceLaunchInfo(@Param("launchId") Long launchId);
}
