package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目驻场人员 Mapper
 */
@Mapper
public interface ProjectSiteMemberMapper extends BaseMapperX<ProjectSiteMemberDO> {

    /**
     * 根据驻场点ID查询人员列表
     *
     * @param siteId 驻场点ID
     * @return 人员列表
     */
    default List<ProjectSiteMemberDO> selectListBySiteId(Long siteId) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId)
                .orderByAsc(ProjectSiteMemberDO::getMemberType)
                .orderByAsc(ProjectSiteMemberDO::getId));
    }

    /**
     * 根据驻场点ID和人员类型查询
     *
     * @param siteId     驻场点ID
     * @param memberType 人员类型
     * @return 人员列表
     */
    default List<ProjectSiteMemberDO> selectListBySiteIdAndType(Long siteId, Integer memberType) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId)
                .eq(ProjectSiteMemberDO::getMemberType, memberType)
                .orderByAsc(ProjectSiteMemberDO::getId));
    }

    /**
     * 根据项目ID查询人员列表
     *
     * @param projectId 项目ID
     * @return 人员列表
     */
    default List<ProjectSiteMemberDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getProjectId, projectId)
                .orderByAsc(ProjectSiteMemberDO::getSiteId)
                .orderByAsc(ProjectSiteMemberDO::getMemberType)
                .orderByAsc(ProjectSiteMemberDO::getId));
    }

    /**
     * 根据用户ID查询参与的项目驻场
     *
     * @param userId 用户ID
     * @return 人员列表
     */
    default List<ProjectSiteMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getUserId, userId)
                .orderByDesc(ProjectSiteMemberDO::getId));
    }

    /**
     * 统计驻场点的在岗人员数量
     *
     * @param siteId 驻场点ID
     * @return 数量
     */
    default Long countActiveBySiteId(Long siteId) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId)
                .eq(ProjectSiteMemberDO::getStatus, ProjectSiteMemberDO.STATUS_ACTIVE));
    }

    /**
     * 统计驻场点的管理人员数量
     *
     * @param siteId 驻场点ID
     * @return 数量
     */
    default Long countManagementBySiteId(Long siteId) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId)
                .eq(ProjectSiteMemberDO::getMemberType, ProjectSiteMemberDO.MEMBER_TYPE_MANAGEMENT)
                .eq(ProjectSiteMemberDO::getStatus, ProjectSiteMemberDO.STATUS_ACTIVE));
    }

    /**
     * 统计驻场点的驻场人员数量
     *
     * @param siteId 驻场点ID
     * @return 数量
     */
    default Long countOnsiteBySiteId(Long siteId) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId)
                .eq(ProjectSiteMemberDO::getMemberType, ProjectSiteMemberDO.MEMBER_TYPE_ONSITE)
                .eq(ProjectSiteMemberDO::getStatus, ProjectSiteMemberDO.STATUS_ACTIVE));
    }

    /**
     * 删除驻场点下的所有人员
     *
     * @param siteId 驻场点ID
     */
    default void deleteBySiteId(Long siteId) {
        delete(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getSiteId, siteId));
    }

    /**
     * 根据驻场点ID列表批量查询人员
     *
     * @param siteIds 驻场点ID列表
     * @return 人员列表
     */
    default List<ProjectSiteMemberDO> selectListBySiteIds(List<Long> siteIds) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .in(ProjectSiteMemberDO::getSiteId, siteIds)
                .orderByAsc(ProjectSiteMemberDO::getSiteId)
                .orderByAsc(ProjectSiteMemberDO::getMemberType));
    }

    /**
     * 查询用户当前是否有"在岗"的驻场分配
     */
    default boolean isUserOnSite(Long userId) {
        return selectCount(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .eq(ProjectSiteMemberDO::getUserId, userId)
                .eq(ProjectSiteMemberDO::getStatus, ProjectSiteMemberDO.STATUS_ACTIVE)) > 0;
    }

    /**
     * 批量查询多个用户的在岗驻场状态
     */
    default List<ProjectSiteMemberDO> selectActiveByUserIds(List<Long> userIds) {
        return selectList(new LambdaQueryWrapperX<ProjectSiteMemberDO>()
                .in(ProjectSiteMemberDO::getUserId, userIds)
                .eq(ProjectSiteMemberDO::getStatus, ProjectSiteMemberDO.STATUS_ACTIVE));
    }

}
