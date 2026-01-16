package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundTargetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 轮次测试目标 Mapper
 */
@Mapper
public interface ProjectRoundTargetMapper extends BaseMapperX<ProjectRoundTargetDO> {

    /**
     * 根据轮次ID查询目标列表
     */
    default List<ProjectRoundTargetDO> selectListByRoundId(Long roundId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundTargetDO>()
                .eq(ProjectRoundTargetDO::getRoundId, roundId)
                .orderByAsc(ProjectRoundTargetDO::getSort));
    }

    /**
     * 根据项目ID查询所有目标
     */
    default List<ProjectRoundTargetDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundTargetDO>()
                .eq(ProjectRoundTargetDO::getProjectId, projectId)
                .orderByAsc(ProjectRoundTargetDO::getSort));
    }

    /**
     * 统计轮次的目标数量
     */
    default Long selectCountByRoundId(Long roundId) {
        return selectCount(new LambdaQueryWrapperX<ProjectRoundTargetDO>()
                .eq(ProjectRoundTargetDO::getRoundId, roundId));
    }

    /**
     * 获取轮次的最大排序值
     */
    default Integer selectMaxSort(Long roundId) {
        ProjectRoundTargetDO target = selectOne(new LambdaQueryWrapperX<ProjectRoundTargetDO>()
                .eq(ProjectRoundTargetDO::getRoundId, roundId)
                .orderByDesc(ProjectRoundTargetDO::getSort)
                .last("LIMIT 1"));
        return target != null ? target.getSort() : 0;
    }

}
