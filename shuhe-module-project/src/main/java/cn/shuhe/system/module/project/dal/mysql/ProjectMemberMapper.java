package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目成员 Mapper
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapperX<ProjectMemberDO> {

    default List<ProjectMemberDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId)
                .orderByAsc(ProjectMemberDO::getRoleType));
    }

    default ProjectMemberDO selectByProjectIdAndUserId(Long projectId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId)
                .eq(ProjectMemberDO::getUserId, userId));
    }

    default void deleteByProjectId(Long projectId) {
        delete(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getProjectId, projectId));
    }

    default List<Long> selectProjectIdsByUserId(Long userId) {
        List<ProjectMemberDO> members = selectList(new LambdaQueryWrapperX<ProjectMemberDO>()
                .eq(ProjectMemberDO::getUserId, userId));
        return members.stream().map(ProjectMemberDO::getProjectId).toList();
    }

}
