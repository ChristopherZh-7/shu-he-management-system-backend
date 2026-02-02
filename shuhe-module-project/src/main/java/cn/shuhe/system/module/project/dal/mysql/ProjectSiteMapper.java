package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目驻场点 Mapper
 */
@Mapper
public interface ProjectSiteMapper extends BaseMapperX<ProjectSiteDO> {

    /**
     * 根据项目ID和部门类型查询驻场点列表
     *
     * @param projectId 项目ID
     * @param deptType  部门类型：1-安全服务 2-安全运营 3-数据安全
     * @return 驻场点列表
     */
    default List<ProjectSiteDO> selectListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId)
                .eq(ProjectSiteDO::getDeptType, deptType)
                .orderByAsc(ProjectSiteDO::getSort)
                .orderByAsc(ProjectSiteDO::getId));
    }

    /**
     * 根据项目ID查询驻场点列表（所有部门）
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    default List<ProjectSiteDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId)
                .orderByAsc(ProjectSiteDO::getSort)
                .orderByAsc(ProjectSiteDO::getId));
    }

    /**
     * 根据项目ID和部门类型查询启用状态的驻场点列表
     *
     * @param projectId 项目ID
     * @param deptType  部门类型
     * @return 驻场点列表
     */
    default List<ProjectSiteDO> selectEnabledListByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId)
                .eq(ProjectSiteDO::getDeptType, deptType)
                .eq(ProjectSiteDO::getStatus, ProjectSiteDO.STATUS_ENABLED)
                .orderByAsc(ProjectSiteDO::getSort)
                .orderByAsc(ProjectSiteDO::getId));
    }

    /**
     * 统计驻场点数量
     *
     * @param projectId 项目ID
     * @param deptType  部门类型
     * @return 数量
     */
    default Long selectCountByProjectIdAndDeptType(Long projectId, Integer deptType) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId)
                .eq(ProjectSiteDO::getDeptType, deptType));
    }

    /**
     * 根据项目ID列表批量查询驻场点
     *
     * @param projectIds 项目ID列表
     * @return 驻场点列表
     */
    default List<ProjectSiteDO> selectListByProjectIds(List<Long> projectIds) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteDO>()
                .in(ProjectSiteDO::getProjectId, projectIds)
                .eq(ProjectSiteDO::getStatus, ProjectSiteDO.STATUS_ENABLED)
                .orderByAsc(ProjectSiteDO::getProjectId)
                .orderByAsc(ProjectSiteDO::getSort));
    }

}
