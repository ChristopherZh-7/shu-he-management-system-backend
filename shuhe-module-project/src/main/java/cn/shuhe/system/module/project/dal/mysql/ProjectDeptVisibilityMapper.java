package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptVisibilityDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 项目部门可见性 Mapper
 */
@Mapper
public interface ProjectDeptVisibilityMapper extends BaseMapperX<ProjectDeptVisibilityDO> {

    default List<ProjectDeptVisibilityDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectDeptVisibilityDO>()
                .eq(ProjectDeptVisibilityDO::getProjectId, projectId)
                .orderByAsc(ProjectDeptVisibilityDO::getDeptId));
    }

    default ProjectDeptVisibilityDO selectByProjectIdAndDeptId(Long projectId, Long deptId) {
        return selectOne(new LambdaQueryWrapperX<ProjectDeptVisibilityDO>()
                .eq(ProjectDeptVisibilityDO::getProjectId, projectId)
                .eq(ProjectDeptVisibilityDO::getDeptId, deptId));
    }

    default void deleteByProjectId(Long projectId) {
        delete(new LambdaQueryWrapperX<ProjectDeptVisibilityDO>()
                .eq(ProjectDeptVisibilityDO::getProjectId, projectId));
    }

    /**
     * 按用户 dept_id（含其所属的多级父部门 / 子部门 id 集合）查项目 id 列表
     * 注：调用方需自行展开「我属于哪些 dept」的集合再传入；本方法只做简单 in 查询。
     */
    default List<Long> selectProjectIdsByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<ProjectDeptVisibilityDO> rows = selectList(
                new LambdaQueryWrapperX<ProjectDeptVisibilityDO>()
                        .in(ProjectDeptVisibilityDO::getDeptId, deptIds));
        return rows.stream().map(ProjectDeptVisibilityDO::getProjectId).distinct().toList();
    }
}
