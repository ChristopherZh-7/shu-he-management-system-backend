package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 项目轮次 Mapper
 */
@Mapper
public interface ProjectRoundMapper extends BaseMapperX<ProjectRoundDO> {

    /**
     * 根据项目ID查询轮次列表
     */
    default List<ProjectRoundDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId)
                .orderByAsc(ProjectRoundDO::getRoundNo));
    }

    /**
     * 获取项目的最大轮次序号
     */
    default Integer selectMaxRoundNo(Long projectId) {
        ProjectRoundDO round = selectOne(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId)
                .orderByDesc(ProjectRoundDO::getRoundNo)
                .last("LIMIT 1"));
        return round != null ? round.getRoundNo() : 0;
    }

    /**
     * 统计项目的轮次数量
     */
    default Long selectCountByProjectId(Long projectId) {
        return selectCount(new LambdaQueryWrapperX<ProjectRoundDO>()
                .eq(ProjectRoundDO::getProjectId, projectId));
    }

}
