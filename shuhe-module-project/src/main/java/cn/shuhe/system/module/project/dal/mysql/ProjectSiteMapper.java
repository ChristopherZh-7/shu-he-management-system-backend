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
     * 根据项目ID查询驻场点列表
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
     * 根据项目ID查询启用状态的驻场点列表
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    default List<ProjectSiteDO> selectEnabledListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId)
                .eq(ProjectSiteDO::getStatus, ProjectSiteDO.STATUS_ENABLED)
                .orderByAsc(ProjectSiteDO::getSort)
                .orderByAsc(ProjectSiteDO::getId));
    }

    /**
     * 统计驻场点数量
     *
     * @param projectId 项目ID
     * @return 数量
     */
    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteDO>()
                .eq(ProjectSiteDO::getProjectId, projectId));
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
