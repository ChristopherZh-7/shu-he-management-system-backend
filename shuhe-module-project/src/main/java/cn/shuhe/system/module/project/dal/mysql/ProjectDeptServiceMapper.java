package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目-部门服务单 Mapper
 */
@Mapper
public interface ProjectDeptServiceMapper extends BaseMapperX<ProjectDeptServiceDO> {

    /**
     * 根据部门类型分页查询
     */
    default PageResult<ProjectDeptServiceDO> selectPage(ProjectDeptServicePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .eqIfPresent(ProjectDeptServiceDO::getDeptType, reqVO.getDeptType())
                .eqIfPresent(ProjectDeptServiceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProjectDeptServiceDO::getClaimed, reqVO.getClaimed())
                .likeIfPresent(ProjectDeptServiceDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(ProjectDeptServiceDO::getContractNo, reqVO.getContractNo())
                .orderByDesc(ProjectDeptServiceDO::getId));
    }

    /**
     * 根据项目ID查询部门服务单列表
     */
    default List<ProjectDeptServiceDO> selectListByProjectId(Long projectId) {
        return selectList(ProjectDeptServiceDO::getProjectId, projectId);
    }

    /**
     * 根据项目ID和部门类型查询
     */
    default ProjectDeptServiceDO selectByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectOne(new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .eq(ProjectDeptServiceDO::getProjectId, projectId)
                .eq(ProjectDeptServiceDO::getDeptType, deptType));
    }

    /**
     * 根据合同ID查询部门服务单列表
     */
    default List<ProjectDeptServiceDO> selectListByContractId(Long contractId) {
        return selectList(ProjectDeptServiceDO::getContractId, contractId);
    }

    /**
     * 根据合同ID和部门类型查询
     */
    default ProjectDeptServiceDO selectByContractIdAndDeptType(Long contractId, Integer deptType) {
        return selectOne(new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .eq(ProjectDeptServiceDO::getContractId, contractId)
                .eq(ProjectDeptServiceDO::getDeptType, deptType));
    }

    /**
     * 根据部门类型查询待领取的服务单
     */
    default List<ProjectDeptServiceDO> selectUnclaimedByDeptType(Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .eq(ProjectDeptServiceDO::getDeptType, deptType)
                .eq(ProjectDeptServiceDO::getClaimed, false)
                .orderByDesc(ProjectDeptServiceDO::getId));
    }

    /**
     * 根据项目ID列表分页查询（用于项目成员权限过滤）
     */
    default PageResult<ProjectDeptServiceDO> selectPageByProjectIds(ProjectDeptServicePageReqVO reqVO, List<Long> projectIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .in(ProjectDeptServiceDO::getProjectId, projectIds)
                .eqIfPresent(ProjectDeptServiceDO::getDeptType, reqVO.getDeptType())
                .eqIfPresent(ProjectDeptServiceDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProjectDeptServiceDO::getClaimed, reqVO.getClaimed())
                .likeIfPresent(ProjectDeptServiceDO::getCustomerName, reqVO.getCustomerName())
                .likeIfPresent(ProjectDeptServiceDO::getContractNo, reqVO.getContractNo())
                .orderByDesc(ProjectDeptServiceDO::getId));
    }

    /**
     * 查询某用户作为负责人的项目ID列表（manager_ids JSON 包含该用户ID）
     */
    @Select("SELECT DISTINCT project_id FROM project_dept_service " +
            "WHERE deleted = 0 AND manager_ids IS NOT NULL " +
            "AND JSON_CONTAINS(manager_ids, CAST(#{userId} AS CHAR), '$')")
    List<Long> selectProjectIdsByManagerUserId(@Param("userId") Long userId);

    /**
     * 根据部门ID查询服务单列表
     */
    default List<ProjectDeptServiceDO> selectListByDeptId(Long deptId) {
        return selectList(ProjectDeptServiceDO::getDeptId, deptId);
    }

    /**
     * 根据合同ID和部门ID查询（用于预算查询，领取后 deptId 才有值）
     */
    default ProjectDeptServiceDO selectByContractIdAndDeptId(Long contractId, Long deptId) {
        return selectOne(new LambdaQueryWrapperX<ProjectDeptServiceDO>()
                .eq(ProjectDeptServiceDO::getContractId, contractId)
                .eq(ProjectDeptServiceDO::getDeptId, deptId));
    }

}
