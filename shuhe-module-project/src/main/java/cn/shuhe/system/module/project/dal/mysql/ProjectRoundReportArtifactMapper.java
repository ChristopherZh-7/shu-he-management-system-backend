package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundReportArtifactDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectRoundReportArtifactMapper extends BaseMapperX<ProjectRoundReportArtifactDO> {
    default List<ProjectRoundReportArtifactDO> selectListByRoundId(Long roundId) {
        return selectList(new LambdaQueryWrapperX<ProjectRoundReportArtifactDO>()
                .eq(ProjectRoundReportArtifactDO::getRoundId, roundId)
                .orderByDesc(ProjectRoundReportArtifactDO::getVersionNo));
    }

    default ProjectRoundReportArtifactDO selectLatest(Long roundId) {
        return selectOne(new LambdaQueryWrapperX<ProjectRoundReportArtifactDO>()
                .eq(ProjectRoundReportArtifactDO::getRoundId, roundId)
                .orderByDesc(ProjectRoundReportArtifactDO::getVersionNo)
                .last("LIMIT 1"));
    }
}
