package cn.shuhe.system.module.system.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 外出申请信息查询 Mapper（跨模块查询）
 */
@Mapper
public interface OutsideRequestInfoMapper {

    /**
     * 查询外出申请信息
     */
    @Select("SELECT r.id, r.project_id AS projectId, r.service_item_id AS serviceItemId, " +
            "r.request_user_id AS requestUserId, r.request_dept_id AS requestDeptId, " +
            "r.target_dept_id AS targetDeptId, r.destination, r.reason, " +
            "r.plan_start_time AS planStartTime, r.plan_end_time AS planEndTime, " +
            "r.status, " +
            "p.contract_id AS contractId, p.name AS serviceItemName, " +
            "c.no AS contractNo, c.name AS contractName, " +
            "req_dept.name AS requestDeptName, " +
            "target_dept.name AS targetDeptName, " +
            "req_user.nickname AS requestUserName " +
            "FROM project_outside_request r " +
            "LEFT JOIN project_info p ON r.service_item_id = p.id AND p.deleted = 0 " +
            "LEFT JOIN crm_contract c ON p.contract_id = c.id AND c.deleted = 0 " +
            "LEFT JOIN system_dept req_dept ON r.request_dept_id = req_dept.id AND req_dept.deleted = 0 " +
            "LEFT JOIN system_dept target_dept ON r.target_dept_id = target_dept.id AND target_dept.deleted = 0 " +
            "LEFT JOIN system_users req_user ON r.request_user_id = req_user.id AND req_user.deleted = 0 " +
            "WHERE r.id = #{requestId} AND r.deleted = 0")
    Map<String, Object> selectOutsideRequestInfo(@Param("requestId") Long requestId);

    /**
     * 查询用户信息
     */
    @Select("SELECT id, nickname AS name, dept_id AS deptId FROM system_users WHERE id = #{userId} AND deleted = 0")
    Map<String, Object> selectUserInfo(@Param("userId") Long userId);

    /**
     * 查询部门信息
     */
    @Select("SELECT id, name, leader_user_id AS leaderUserId, parent_id AS parentId FROM system_dept WHERE id = #{deptId} AND deleted = 0")
    Map<String, Object> selectDeptInfo(@Param("deptId") Long deptId);

    /**
     * 查询部门主管信息
     */
    @Select("SELECT u.id, u.nickname AS name, u.dept_id AS deptId, d.name AS deptName " +
            "FROM system_dept d " +
            "LEFT JOIN system_users u ON d.leader_user_id = u.id AND u.deleted = 0 " +
            "WHERE d.id = #{deptId} AND d.deleted = 0 AND d.leader_user_id IS NOT NULL")
    Map<String, Object> selectDeptLeader(@Param("deptId") Long deptId);
}
